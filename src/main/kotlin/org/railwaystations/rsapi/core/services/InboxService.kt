package org.railwaystations.rsapi.core.services

import org.apache.commons.lang3.StringUtils
import org.railwaystations.rsapi.adapter.db.CountryDao
import org.railwaystations.rsapi.adapter.db.InboxDao
import org.railwaystations.rsapi.adapter.db.PhotoDao
import org.railwaystations.rsapi.adapter.db.StationDao
import org.railwaystations.rsapi.adapter.db.UserDao
import org.railwaystations.rsapi.core.model.Coordinates
import org.railwaystations.rsapi.core.model.Country
import org.railwaystations.rsapi.core.model.InboxCommand
import org.railwaystations.rsapi.core.model.InboxEntry
import org.railwaystations.rsapi.core.model.InboxEntry.Companion.createFilename
import org.railwaystations.rsapi.core.model.InboxResponse
import org.railwaystations.rsapi.core.model.InboxStateQuery
import org.railwaystations.rsapi.core.model.InboxStateQuery.InboxState
import org.railwaystations.rsapi.core.model.License
import org.railwaystations.rsapi.core.model.Photo
import org.railwaystations.rsapi.core.model.ProblemReport
import org.railwaystations.rsapi.core.model.PublicInboxEntry
import org.railwaystations.rsapi.core.model.Station
import org.railwaystations.rsapi.core.model.User
import org.railwaystations.rsapi.core.ports.ManageInboxUseCase
import org.railwaystations.rsapi.core.ports.MastodonBot
import org.railwaystations.rsapi.core.ports.Monitor
import org.railwaystations.rsapi.core.ports.PhotoStorage
import org.railwaystations.rsapi.core.ports.PhotoStorage.PhotoTooLargeException
import org.railwaystations.rsapi.utils.ImageUtil.mimeToExtension
import org.railwaystations.rsapi.utils.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.util.UriUtils
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.util.*
import java.util.function.Consumer

@Service
class InboxService(
    private val stationDao: StationDao,
    private val photoStorage: PhotoStorage,
    private val monitor: Monitor,
    private val inboxDao: InboxDao,
    private val userDao: UserDao,
    private val countryDao: CountryDao,
    private val photoDao: PhotoDao,
    @param:Value("\${inboxBaseUrl}") private val inboxBaseUrl: String,
    private val mastodonBot: MastodonBot,
    @param:Value("\${photoBaseUrl}") private val photoBaseUrl: String,
    private val clock: Clock,
    @param:Value(
        "\${mastodon-bot.stationUrl}"
    ) private val stationUrl: String
) : ManageInboxUseCase {

    private val log by Logger()

    override fun reportProblem(problemReport: ProblemReport, user: User, clientInfo: String?): InboxResponse {
        if (!user.isEligibleToReportProblem) {
            log.info("New problem report failed for user {}, profile incomplete", user.name)
            return InboxResponse(state = InboxResponse.InboxResponseState.UNAUTHORIZED, message = "Profile incomplete")
        }

        log.info(
            "New problem report: Nickname: {}; Country: {}; Station-Id: {}",
            user.name, problemReport.countryCode, problemReport.stationId
        )

        val station = findStationByCountryAndId(problemReport.countryCode, problemReport.stationId)
            ?: return InboxResponse(
                state = InboxResponse.InboxResponseState.NOT_ENOUGH_DATA,
                message = "Station not found"
            )

        if (StringUtils.isBlank(problemReport.comment)) {
            return InboxResponse(
                state = InboxResponse.InboxResponseState.NOT_ENOUGH_DATA,
                message = "Comment is mandatory"
            )
        }

        var photoId = problemReport.photoId
        if (problemReport.type.needsPhoto()) {
            if (!station.hasPhoto()) {
                return InboxResponse(
                    state = InboxResponse.InboxResponseState.NOT_ENOUGH_DATA,
                    message = "Problem type is only applicable to station with photo"
                )
            }
            if (photoId != null) {
                if (station.photos.none { photo: Photo -> photo.id == problemReport.photoId }) {
                    return InboxResponse(
                        state = InboxResponse.InboxResponseState.NOT_ENOUGH_DATA,
                        message = "Photo with this id not found at station"
                    )
                }
            } else {
                val primaryPhoto = station.primaryPhoto
                photoId = primaryPhoto?.id
            }
        }
        val inboxEntry = InboxEntry(
            countryCode = problemReport.countryCode,
            stationId = problemReport.stationId,
            photoId = photoId,
            title = problemReport.title,
            coordinates = problemReport.coordinates,
            photographerId = user.id,
            comment = problemReport.comment,
            createdAt = clock.instant(),
            problemReportType = problemReport.type,
        )
        monitor.sendMessage(
            "New problem report for ${station.title} - ${station.key.country}:${station.key.id}\n${problemReport.type}: ${problemReport.comment?.trim() ?: ""}\nby ${user.name}\nvia $clientInfo"
        )
        return InboxResponse(state = InboxResponse.InboxResponseState.REVIEW, id = inboxDao.insert(inboxEntry))
    }

    override fun publicInbox(): List<PublicInboxEntry> {
        return inboxDao.findPublicInboxEntries()
    }

    override fun userInbox(user: User): List<InboxStateQuery> {
        return inboxDao.findByUser(user.id)
            .map { inboxEntry: InboxEntry -> mapToInboxStateQuery(inboxEntry) }
    }

    override fun userInbox(user: User, ids: List<Long>): List<InboxStateQuery> {
        return ids
            .mapNotNull { id -> inboxDao.findById(id) }
            .filter { inboxEntry -> inboxEntry.photographerId == user.id }
            .map { inboxEntry -> mapToInboxStateQuery(inboxEntry) }
    }

    private fun mapToInboxStateQuery(inboxEntry: InboxEntry): InboxStateQuery {
        inboxEntry.processed =
            !inboxEntry.done && (inboxEntry.filename == null || photoStorage.isProcessed(inboxEntry.filename!!))
        return InboxStateQuery(
            id = inboxEntry.id,
            countryCode = inboxEntry.countryCode,
            stationId = inboxEntry.stationId,
            title = inboxEntry.title,
            coordinates = inboxEntry.coordinates,
            newTitle = inboxEntry.newTitle,
            newCoordinates = inboxEntry.newCoordinates,
            state = calculateUserInboxState(inboxEntry),
            comment = inboxEntry.comment,
            problemReportType = inboxEntry.problemReportType,
            rejectedReason = inboxEntry.rejectReason,
            filename = inboxEntry.filename,
            inboxUrl = getInboxUrl(inboxEntry),
            crc32 = inboxEntry.crc32,
            createdAt = inboxEntry.createdAt
        )
    }

    private fun calculateUserInboxState(inboxEntry: InboxEntry): InboxState {
        return if (inboxEntry.done) {
            if (inboxEntry.rejectReason == null) {
                InboxState.ACCEPTED
            } else {
                InboxState.REJECTED
            }
        } else {
            InboxState.REVIEW
        }
    }

    override fun listAdminInbox(user: User): List<InboxEntry> {
        val pendingInboxEntries = inboxDao.findPendingInboxEntries()
        pendingInboxEntries.forEach(Consumer { inboxEntry -> this.updateInboxEntry(inboxEntry) })
        return pendingInboxEntries
    }

    private fun updateInboxEntry(inboxEntry: InboxEntry) {
        val filename = inboxEntry.filename
        if (filename != null) {
            inboxEntry.processed = photoStorage.isProcessed(filename)
            inboxEntry.inboxUrl = getInboxUrl(inboxEntry)
        } else if (inboxEntry.hasPhoto()) {
            inboxEntry.inboxUrl = photoBaseUrl + inboxEntry.existingPhotoUrlPath
        }
        if (inboxEntry.stationId == null && !inboxEntry.newCoordinates!!.hasZeroCoords()) {
            inboxEntry.conflict = hasConflict(inboxEntry.id, inboxEntry.newCoordinates)
        }
    }

    private fun getInboxUrl(inboxEntry: InboxEntry): String? {
        if (inboxEntry.filename == null) {
            return null
        }

        if (inboxEntry.done) {
            return if (inboxEntry.rejectReason != null) {
                inboxBaseUrl + "/rejected/" + inboxEntry.filename
            } else {
                inboxBaseUrl + "/done/" + inboxEntry.filename
            }
        }
        return inboxBaseUrl + (if (inboxEntry.processed) "/processed/" else "/") + inboxEntry.filename
    }

    override fun markPhotoOutdated(command: InboxCommand) {
        val inboxEntry = assertPendingInboxEntryExists(command)
        val station = assertStationExistsAndHasPhoto(inboxEntry)
        photoDao.updatePhotoOutdated(getPhotoIdFromInboxOrPrimaryPhoto(inboxEntry, station))
        inboxDao.done(inboxEntry.id)
    }

    override fun deleteUserInboxEntry(user: User, id: Long) {
        val inboxEntry = inboxDao.findById(id) ?: throw ManageInboxUseCase.InboxEntryNotFoundException()
        require(!inboxEntry.done) { "InboxEntry is already done" }
        if (inboxEntry.photographerId != user.id) {
            throw ManageInboxUseCase.InboxEntryNotOwnerException()
        }
        inboxDao.reject(id, "Withdrawn by user")
        monitor.sendMessage("InboxEntry $id ${inboxEntry.title} has been withdrawn by ${user.name}")
    }

    override fun updateLocation(command: InboxCommand) {
        val inboxEntry = assertPendingInboxEntryExists(command)
        val coordinates = command.coordinates
        require(!(coordinates == null || !coordinates.isValid)) { "Can't update location, coordinates: " + command.coordinates }

        val station = assertStationExists(inboxEntry)
        stationDao.updateLocation(station.key, coordinates)
        inboxDao.done(inboxEntry.id)
    }

    override fun countPendingInboxEntries(): Long {
        return inboxDao.countPendingInboxEntries()
    }

    override val nextZ: String
        get() = "Z${stationDao.maxZ + 1}"

    override fun updateStationActiveState(command: InboxCommand, active: Boolean) {
        val inboxEntry = assertPendingInboxEntryExists(command)
        val station = assertStationExists(inboxEntry)
        stationDao.updateActive(station.key, active)
        inboxDao.done(inboxEntry.id)
        log.info("Problem report {} station {} set active to {}", inboxEntry.id, station.key, active)
    }

    override fun changeStationTitle(command: InboxCommand) {
        val inboxEntry = assertPendingInboxEntryExists(command)
        require(!StringUtils.isBlank(command.title)) { "Empty new title: " + command.title }
        val station = assertStationExists(inboxEntry)
        stationDao.changeStationTitle(station.key, command.title!!)
        inboxDao.done(inboxEntry.id)
        log.info(
            "Problem report {} station {} changed name to {}",
            inboxEntry.id,
            station.key,
            command.title
        )
    }

    private fun assertPendingInboxEntryExists(command: InboxCommand): InboxEntry {
        val inboxEntry = inboxDao.findById(command.id)
        require(inboxEntry != null && !inboxEntry.done) { "No pending inbox entry found" }
        return inboxEntry
    }

    override fun deleteStation(command: InboxCommand) {
        val inboxEntry = assertPendingInboxEntryExists(command)
        val station = assertStationExists(inboxEntry)
        stationDao.delete(station.key)
        inboxDao.done(inboxEntry.id)
        log.info("Problem report {} station {} deleted", inboxEntry.id, station.key)
    }

    override fun deletePhoto(command: InboxCommand) {
        val inboxEntry = assertPendingInboxEntryExists(command)
        val station = assertStationExistsAndHasPhoto(inboxEntry)
        val photoIdToDelete = getPhotoIdFromInboxOrPrimaryPhoto(inboxEntry, station)
        photoDao.delete(photoIdToDelete)
        val primaryPhoto = station.primaryPhoto
        if (primaryPhoto != null && primaryPhoto.id == photoIdToDelete) {
            station.photos.firstOrNull { it.id != photoIdToDelete }
                ?.let { photo -> photoDao.setPrimary(photo.id) }
        }
        inboxDao.done(inboxEntry.id)
        log.info("Problem report {} photo of station {} deleted", inboxEntry.id, station.key)
    }

    private fun getPhotoIdFromInboxOrPrimaryPhoto(inboxEntry: InboxEntry, station: Station): Long {
        if (inboxEntry.photoId != null) {
            return inboxEntry.photoId!!
        }

        val primaryPhoto = station.primaryPhoto
        if (primaryPhoto != null) {
            return station.primaryPhoto!!.id
        }

        throw IllegalArgumentException("Station has no primary photo")
    }

    override fun markProblemReportSolved(command: InboxCommand) {
        val inboxEntry = assertPendingInboxEntryExists(command)
        inboxDao.done(inboxEntry.id)
        log.info("Problem report {} resolved", inboxEntry.id)
    }

    private fun assertStationExists(inboxEntry: InboxEntry): Station {
        return findStationByCountryAndId(inboxEntry.countryCode, inboxEntry.stationId)
            ?: throw IllegalArgumentException("Station not found")
    }

    private fun assertStationExistsAndHasPhoto(inboxEntry: InboxEntry): Station {
        val station = assertStationExists(inboxEntry)
        require(station.hasPhoto()) { "Station has no photo" }
        return station
    }

    override fun importMissingStation(command: InboxCommand) {
        val inboxEntry = assertPendingInboxEntryExists(command)
        log.info("Importing photo {}, {}", inboxEntry.id, inboxEntry.filename)

        require(!inboxEntry.isProblemReport) { "Can't import a problem report" }

        val station = findOrCreateStation(command)

        if (inboxEntry.isPhotoUpload) {
            importPhoto(command, inboxEntry, station)
        } else {
            log.info("No photo to import for InboxEntry={}", inboxEntry.id)
        }
        inboxDao.updateMissingStationImported(inboxEntry.id, station.key.country, station.key.id, station.title)
    }

    override fun importPhoto(command: InboxCommand) {
        val inboxEntry = assertPendingInboxEntryExists(command)
        log.info("Importing photo {}, {}", inboxEntry.id, inboxEntry.filename)

        require(inboxEntry.isPhotoUpload) { "No photo to import" }

        val station = findStationByCountryAndId(inboxEntry.countryCode, inboxEntry.stationId)
            ?: throw IllegalArgumentException("Station not found")

        importPhoto(command, inboxEntry, station)
        inboxDao.done(inboxEntry.id)
    }

    private fun importPhoto(command: InboxCommand, inboxEntry: InboxEntry, station: Station) {
        if (hasConflict(inboxEntry.id, station)) {
            require(command.conflictResolution!!.solvesPhotoConflict()) { "There is a conflict with another photo" }
            require(!(!station.hasPhoto() && command.conflictResolution != InboxCommand.ConflictResolution.IMPORT_AS_NEW_PRIMARY_PHOTO)) { "Conflict with another upload! The only possible ConflictResolution strategy is IMPORT_AS_NEW_PRIMARY_PHOTO." }
        }

        val photographer = userDao.findById(inboxEntry.photographerId)
            ?: throw IllegalArgumentException("Photographer " + inboxEntry.photographerId + " not found")
        val country = countryDao.findById(StringUtils.lowerCase(station.key.country))
            ?: throw IllegalArgumentException("Country " + station.key.country + " not found")

        try {
            val urlPath = photoStorage.importPhoto(inboxEntry, station)

            val photo = Photo(
                id = 0,
                stationKey = station.key,
                primary = false,
                urlPath = urlPath,
                photographer = photographer,
                createdAt = Instant.now(),
                license = getLicenseForPhoto(photographer, country),
                outdated = false
            )
            val photoId: Long
            if (station.hasPhoto()) {
                when (command.conflictResolution) {
                    InboxCommand.ConflictResolution.IMPORT_AS_NEW_PRIMARY_PHOTO -> {
                        photoDao.setAllPhotosForStationSecondary(station.key)
                        photo.primary = true
                        photoId = photoDao.insert(photo)
                    }

                    InboxCommand.ConflictResolution.IMPORT_AS_NEW_SECONDARY_PHOTO -> {
                        photo.primary = false
                        photoId = photoDao.insert(photo)
                    }

                    InboxCommand.ConflictResolution.OVERWRITE_EXISTING_PHOTO -> {
                        val primaryPhoto = station.primaryPhoto
                            ?: throw IllegalArgumentException("Station has no primary photo to overwrite")
                        photoId = primaryPhoto.id
                        photo.id = photoId
                        photo.primary = true
                        photoDao.update(photo)
                    }

                    else -> throw IllegalArgumentException("No suitable conflict resolution provided")
                }
            } else {
                photo.primary = true
                photoId = photoDao.insert(photo)
            }

            inboxDao.updatePhotoId(inboxEntry.id, photoId)
            log.info("Upload {} with photoId {} accepted: {}", inboxEntry.id, photoId, inboxEntry.filename)
        } catch (e: Exception) {
            log.error("Error importing upload {} photo {}", inboxEntry.id, inboxEntry.filename)
            throw RuntimeException("Error moving file", e)
        }
    }

    private fun findOrCreateStation(command: InboxCommand): Station {
        require(command.countryCode != null && command.stationId != null) { "CountryCode and StationId required" }
        val station = findStationByCountryAndId(command.countryCode, command.stationId)

        if (station != null) {
            return station
        }

        // create station
        val country = countryDao.findById(StringUtils.lowerCase(command.countryCode))
        require(country != null) { "Country not found" }
        require(command.stationId!!.startsWith("Z")) { "Station ID can't be empty and must start with Z" }
        require(!(!command.hasCoords() || !command.coordinates!!.isValid)) { "No valid coordinates provided" }
        require(
            !(hasConflict(
                command.id,
                command.coordinates
            ) && !command.conflictResolution!!.solvesStationConflict())
        ) { "There is a conflict with a nearby station" }
        require(!StringUtils.isBlank(command.title)) { "Station title can't be empty" }
        requireNotNull(command.active) { "No Active flag provided" }

        val newStation = Station(
            key = Station.Key(country.code, nextZ),
            title = command.title!!,
            coordinates = command.coordinates!!,
            ds100 = command.ds100,
            photos = mutableListOf(),
            active = command.active!!
        )
        stationDao.insert(newStation)
        log.info("New station '{}' created: {}", newStation.title, newStation.key)
        return newStation
    }

    override fun rejectInboxEntry(command: InboxCommand) {
        val inboxEntry = assertPendingInboxEntryExists(command)
        inboxDao.reject(inboxEntry.id, command.rejectReason!!)
        if (inboxEntry.isProblemReport) {
            log.info("Rejecting problem report {}, {}", inboxEntry.id, command.rejectReason)
            return
        }

        log.info("Rejecting upload {}, {}, {}", inboxEntry.id, command.rejectReason, inboxEntry.filename)

        try {
            photoStorage.reject(inboxEntry)
        } catch (e: IOException) {
            log.warn("Unable to move rejected file {}", inboxEntry.filename, e)
        }
    }

    override fun uploadPhoto(
        clientInfo: String?,
        body: InputStream?,
        stationId: String?,
        countryCode: String?,
        contentType: String?,
        stationTitle: String?,
        latitude: Double?,
        longitude: Double?,
        comment: String?,
        active: Boolean,
        user: User
    ): InboxResponse {
        if (!user.isEligibleToUploadPhoto) {
            log.info("Photo upload failed for user {}, profile incomplete", user.name)
            return InboxResponse(
                state = InboxResponse.InboxResponseState.UNAUTHORIZED,
                message = "Profile incomplete, not allowed to upload photos"
            )
        }

        val station = findStationByCountryAndId(countryCode, stationId)
        val coordinates: Coordinates?
        if (station == null) {
            log.warn("Station not found")
            if (StringUtils.isBlank(stationTitle) || latitude == null || longitude == null) {
                log.warn(
                    "Not enough data for missing station: title={}, latitude={}, longitude={}",
                    stationTitle,
                    latitude,
                    longitude
                )
                return InboxResponse(
                    state = InboxResponse.InboxResponseState.NOT_ENOUGH_DATA,
                    message = "Not enough data: either 'countryCode' and 'stationId' or 'title', 'latitude' and 'longitude' have to be provided"
                )
            }
            coordinates = Coordinates(latitude, longitude)
            if (!coordinates.isValid) {
                log.warn("Lat/Lon out of range: latitude={}, longitude={}", latitude, longitude)
                return InboxResponse(
                    state = InboxResponse.InboxResponseState.LAT_LON_OUT_OF_RANGE,
                    message = "'latitude' and/or 'longitude' out of range"
                )
            }
        } else {
            coordinates = null
        }

        val extension = mimeToExtension(contentType)
        if (station != null && extension == null) {
            log.warn("Unknown contentType '{}'", contentType)
            return InboxResponse(
                state = InboxResponse.InboxResponseState.UNSUPPORTED_CONTENT_TYPE,
                message = "unsupported content type (only jpg and png are supported)"
            )
        }

        val conflict = hasConflict(null, station) || hasConflict(null, coordinates)

        var filename: String? = null
        var inboxUrl: String? = null
        val id: Long
        var crc32: Long? = null
        try {
            val inboxEntry = InboxEntry(
                countryCode = countryCode,
                title = stationTitle,
                coordinates = coordinates,
                photographerId = user.id,
                extension = extension,
                comment = comment,
                createdAt = Instant.now(),
                active = active,
            )

            station?.let { s: Station? ->
                inboxEntry.countryCode = s!!.key.country
                inboxEntry.stationId = s.key.id
            }
            id = inboxDao.insert(inboxEntry)
            if (extension != null) {
                filename = createFilename(id, extension)
                crc32 = photoStorage.storeUpload(body!!, filename)
                inboxDao.updateCrc32(id, crc32)
                inboxUrl = inboxBaseUrl + "/" + UriUtils.encodePath(filename, StandardCharsets.UTF_8)
            }

            val duplicateInfo = if (conflict) " (possible duplicate!)" else ""
            val countryCodeParam = countryCode?.let { "countryCode=$countryCode&" } ?: ""
            if (station != null) {
                monitor.sendMessage(
                    "New photo upload for ${station.title} - ${station.key.country}:${station.key.id}\n${
                        StringUtils.trimToEmpty(
                            comment
                        )
                    }\n$inboxUrl$duplicateInfo\nby ${user.name}\nvia $clientInfo",
                    photoStorage.getUploadFile(filename!!)
                )
            } else if (filename != null) {
                monitor.sendMessage(
                    "Photo upload for missing station $stationTitle at https://map.railway-stations.org/index.php?${countryCodeParam}mlat=$latitude&mlon=$longitude&zoom=18&layers=M\n${
                        StringUtils.trimToEmpty(
                            comment
                        )
                    }\n$inboxUrl$duplicateInfo\nby ${user.name}\nvia $clientInfo",
                    photoStorage.getUploadFile(filename)
                )
            } else {
                monitor.sendMessage(
                    "Report missing station $stationTitle at https://map.railway-stations.org/index.php?${countryCodeParam}mlat=$latitude&mlon=$longitude&zoom=18&layers=M\n${
                        StringUtils.trimToEmpty(
                            comment
                        )
                    }$duplicateInfo\nby ${user.name}\nvia $clientInfo"
                )
            }
        } catch (e: PhotoTooLargeException) {
            return InboxResponse(
                state = InboxResponse.InboxResponseState.PHOTO_TOO_LARGE,
                message = "Photo too large, max " + e.maxSize + " bytes allowed"
            )
        } catch (e: IOException) {
            log.error("Error uploading photo", e)
            return InboxResponse(state = InboxResponse.InboxResponseState.ERROR, message = "Internal Error")
        }

        return InboxResponse(
            id = id,
            state = if (conflict) InboxResponse.InboxResponseState.CONFLICT else InboxResponse.InboxResponseState.REVIEW,
            filename = filename,
            inboxUrl = inboxUrl,
            crc32 = crc32
        )
    }

    private fun hasConflict(id: Long?, station: Station?): Boolean {
        if (station == null) {
            return false
        }
        if (station.hasPhoto()) {
            return true
        }
        return inboxDao.countPendingInboxEntriesForStation(id, station.key.country, station.key.id) > 0
    }

    private fun hasConflict(id: Long?, coordinates: Coordinates?): Boolean {
        if (coordinates == null || coordinates.hasZeroCoords()) {
            return false
        }
        return inboxDao.countPendingInboxEntriesForNearbyCoordinates(
            id,
            coordinates
        ) > 0 || stationDao.countNearbyCoordinates(coordinates) > 0
    }

    private fun findStationByCountryAndId(countryCode: String?, stationId: String?): Station? {
        if (countryCode == null || stationId == null) {
            return null
        }
        return stationDao.findByKey(countryCode, stationId)
    }

    fun postRecentlyImportedPhotoNotYetPosted() {
        val inboxEntriesToPost = inboxDao.findRecentlyImportedPhotosNotYetPosted()
        if (inboxEntriesToPost.isEmpty()) {
            return
        }
        val rand = Random()
        val inboxEntry = inboxEntriesToPost[rand.nextInt(inboxEntriesToPost.size)]
        val photographer = userDao.findById(inboxEntry.photographerId)
        var status =
            "${inboxEntry.title}\nby ${photographer?.displayName ?: User.ANONYM}\n$stationUrl?countryCode=${inboxEntry.countryCode}&stationId=${inboxEntry.stationId}&photoId=${inboxEntry.photoId}"
        if (StringUtils.isNotBlank(inboxEntry.comment)) {
            status += "\n${inboxEntry.comment}"
        }

        mastodonBot.tootNewPhoto(status)
        inboxDao.updatePosted(inboxEntry.id)
    }

    companion object {
        /**
         * Gets the applicable license for the given country.
         * We need to override the license for some countries, because of limitations of the "Freedom of panorama".
         */
        @JvmStatic
        fun getLicenseForPhoto(photographer: User, country: Country): License {
            return country.overrideLicense ?: photographer.license
        }
    }
}
