package org.railwaystations.rsapi.core.services

import org.railwaystations.rsapi.core.model.Coordinates
import org.railwaystations.rsapi.core.model.Country
import org.railwaystations.rsapi.core.model.InboxCommand
import org.railwaystations.rsapi.core.model.InboxEntry
import org.railwaystations.rsapi.core.model.InboxResponse
import org.railwaystations.rsapi.core.model.InboxStateQuery
import org.railwaystations.rsapi.core.model.InboxStateQuery.InboxState
import org.railwaystations.rsapi.core.model.Photo
import org.railwaystations.rsapi.core.model.ProblemReport
import org.railwaystations.rsapi.core.model.PublicInboxEntry
import org.railwaystations.rsapi.core.model.Station
import org.railwaystations.rsapi.core.model.User
import org.railwaystations.rsapi.core.model.createInboxFilename
import org.railwaystations.rsapi.core.ports.inbound.ManageInboxUseCase
import org.railwaystations.rsapi.core.ports.outbound.CountryPort
import org.railwaystations.rsapi.core.ports.outbound.InboxPort
import org.railwaystations.rsapi.core.ports.outbound.MonitorPort
import org.railwaystations.rsapi.core.ports.outbound.PhotoPort
import org.railwaystations.rsapi.core.ports.outbound.PhotoStoragePort
import org.railwaystations.rsapi.core.ports.outbound.PhotoStoragePort.PhotoTooLargeException
import org.railwaystations.rsapi.core.ports.outbound.StationPort
import org.railwaystations.rsapi.core.ports.outbound.UserPort
import org.railwaystations.rsapi.core.utils.ImageUtil.mimeToExtension
import org.railwaystations.rsapi.core.utils.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.util.UriUtils
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant

@Service
class InboxService(
    private val stationPort: StationPort,
    private val photoStoragePort: PhotoStoragePort,
    private val monitorPort: MonitorPort,
    private val inboxPort: InboxPort,
    private val userPort: UserPort,
    private val countryPort: CountryPort,
    private val photoPort: PhotoPort,
    @param:Value("\${inboxBaseUrl}") private val inboxBaseUrl: String,
    @param:Value("\${photoBaseUrl}") private val photoBaseUrl: String,
    private val clock: Clock,
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

        if (problemReport.comment.isNullOrBlank()) {
            return InboxResponse(
                state = InboxResponse.InboxResponseState.NOT_ENOUGH_DATA,
                message = "Comment is mandatory"
            )
        }

        var photoId = problemReport.photoId
        if (problemReport.type.needsPhoto) {
            if (!station.hasPhoto) {
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
        monitorPort.sendMessage(
            "New problem report for ${station.title} - ${station.key.country}:${station.key.id}\n${problemReport.type}: ${problemReport.comment.trim()}\nby ${user.name}\nvia $clientInfo"
        )
        return InboxResponse(state = InboxResponse.InboxResponseState.REVIEW, id = inboxPort.insert(inboxEntry))
    }

    override fun publicInbox(): List<PublicInboxEntry> {
        return inboxPort.findPublicInboxEntries()
    }

    override fun userInbox(user: User, showCompletedEntries: Boolean): List<InboxStateQuery> {
        return inboxPort.findByUser(user.id, showCompletedEntries)
            .map { inboxEntry: InboxEntry -> mapToInboxStateQuery(inboxEntry) }
    }

    override fun userInbox(user: User, ids: List<Long>): List<InboxStateQuery> {
        return ids
            .mapNotNull { id -> inboxPort.findById(id) }
            .filter { inboxEntry -> inboxEntry.photographerId == user.id }
            .map { inboxEntry -> mapToInboxStateQuery(inboxEntry) }
    }

    private fun mapToInboxStateQuery(inboxEntry: InboxEntry): InboxStateQuery {
        val processed =
            !inboxEntry.done && (inboxEntry.filename == null || photoStoragePort.isProcessed(inboxEntry.filename!!))
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
            inboxUrl = getInboxUrl(inboxEntry.filename, inboxEntry.done, inboxEntry.rejectReason, processed),
            crc32 = inboxEntry.crc32,
            createdAt = inboxEntry.createdAt
        )
    }

    private fun calculateUserInboxState(inboxEntry: InboxEntry): InboxState = if (inboxEntry.done) {
        if (inboxEntry.rejectReason == null) {
            InboxState.ACCEPTED
        } else {
            InboxState.REJECTED
        }
    } else {
        InboxState.REVIEW
    }

    override fun listAdminInbox(user: User): List<InboxEntry> {
        val pendingInboxEntries = inboxPort.findPendingInboxEntries()
        return pendingInboxEntries.map { updateInboxEntry(it, pendingInboxEntries) }
    }

    private fun updateInboxEntry(inboxEntry: InboxEntry, pendingInboxEntries: List<InboxEntry>): InboxEntry {
        val filename = inboxEntry.filename
        val processed = filename?.let { photoStoragePort.isProcessed(it) } == true
        val inboxUrl = if (filename != null) {
            getInboxUrl(inboxEntry.filename, inboxEntry.done, inboxEntry.rejectReason, processed)
        } else if (inboxEntry.hasPhoto) {
            photoBaseUrl + inboxEntry.existingPhotoUrlPath
        } else {
            null
        }
        val conflict = if (inboxEntry.stationId == null && !inboxEntry.newCoordinates!!.hasZeroCoords) {
            hasConflict(inboxEntry.id, inboxEntry.newCoordinates)
        } else {
            pendingInboxEntries.hasOtherEntryForSameStation(inboxEntry)
        }

        return inboxEntry.copy(
            processed = processed,
            inboxUrl = inboxUrl,
            conflict = conflict,
        )
    }

    private fun List<InboxEntry>.hasOtherEntryForSameStation(inboxEntry: InboxEntry) =
        any { it.id != inboxEntry.id && it.countryCode == inboxEntry.countryCode && it.stationId == inboxEntry.stationId }

    private fun getInboxUrl(filename: String?, done: Boolean, rejectReason: String?, processed: Boolean): String? {
        if (filename == null) {
            return null
        }

        if (done) {
            return if (rejectReason != null) {
                "$inboxBaseUrl/rejected/$filename"
            } else {
                "$inboxBaseUrl/done/$filename"
            }
        }
        return inboxBaseUrl + (if (processed) "/processed/" else "/") + filename
    }

    override fun markPhotoOutdated(command: InboxCommand) {
        val inboxEntry = assertPendingInboxEntryExists(command)
        val station = assertStationExistsAndHasPhoto(inboxEntry)
        photoPort.updatePhotoOutdated(getPhotoIdFromInboxOrPrimaryPhoto(inboxEntry, station))
        inboxPort.done(inboxEntry.id)
    }

    override fun deleteUserInboxEntry(user: User, id: Long) {
        val inboxEntry = inboxPort.findById(id) ?: throw ManageInboxUseCase.InboxEntryNotFoundException()
        require(!inboxEntry.done) { "InboxEntry is already done" }
        if (inboxEntry.photographerId != user.id) {
            throw ManageInboxUseCase.InboxEntryNotOwnerException()
        }
        inboxPort.reject(id, "Withdrawn by user")
        monitorPort.sendMessage("InboxEntry $id ${inboxEntry.title} has been withdrawn by ${user.name}")
    }

    override fun updateLocation(command: InboxCommand) {
        val inboxEntry = assertPendingInboxEntryExists(command)
        val coordinates = command.coordinates
        require(!(coordinates == null || !coordinates.isValid)) { "Can't update location, coordinates: " + command.coordinates }

        val station = assertStationExists(inboxEntry)
        stationPort.updateLocation(station.key, coordinates)
        inboxPort.done(inboxEntry.id)
    }

    override fun countPendingInboxEntries(): Int {
        return inboxPort.countPendingInboxEntries()
    }

    override fun updateStationActiveState(command: InboxCommand, active: Boolean) {
        val inboxEntry = assertPendingInboxEntryExists(command)
        val station = assertStationExists(inboxEntry)
        stationPort.updateActive(station.key, active)
        inboxPort.done(inboxEntry.id)
        log.info("Problem report {} station {} set active to {}", inboxEntry.id, station.key, active)
    }

    override fun changeStationTitle(command: InboxCommand) {
        val inboxEntry = assertPendingInboxEntryExists(command)
        require(!command.title.isNullOrBlank()) { "Empty new title: " + command.title }
        val station = assertStationExists(inboxEntry)
        stationPort.changeStationTitle(station.key, command.title)
        inboxPort.done(inboxEntry.id)
        log.info(
            "Problem report {} station {} changed name to {}",
            inboxEntry.id,
            station.key,
            command.title
        )
    }

    private fun assertPendingInboxEntryExists(command: InboxCommand): InboxEntry {
        val inboxEntry = inboxPort.findById(command.id)
        require(inboxEntry != null && !inboxEntry.done) { "No pending inbox entry found" }
        return inboxEntry
    }

    override fun deleteStation(command: InboxCommand) {
        val inboxEntry = assertPendingInboxEntryExists(command)
        val station = assertStationExists(inboxEntry)
        stationPort.delete(station.key)
        inboxPort.done(inboxEntry.id)
        inboxPort.findPendingByStation(station.key.country, station.key.id).forEach {
            rejectInboxEntry(
                InboxCommand(
                    id = it.id,
                    countryCode = station.key.country,
                    stationId = station.key.id,
                    rejectReason = "Station has been deleted",
                )
            )
        }
        log.info("Problem report {} station {} deleted", inboxEntry.id, station.key)
    }

    override fun deletePhoto(command: InboxCommand) {
        val inboxEntry = assertPendingInboxEntryExists(command)
        val station = assertStationExistsAndHasPhoto(inboxEntry)
        val photoIdToDelete = getPhotoIdFromInboxOrPrimaryPhoto(inboxEntry, station)
        photoPort.delete(photoIdToDelete)
        val primaryPhoto = station.primaryPhoto
        if (primaryPhoto != null && primaryPhoto.id == photoIdToDelete) {
            station.photos.firstOrNull { it.id != photoIdToDelete }
                ?.let { photo -> photoPort.setPrimary(photo.id) }
        }
        inboxPort.done(inboxEntry.id)
        log.info("Problem report {} photo of station {} deleted", inboxEntry.id, station.key)
    }

    private fun getPhotoIdFromInboxOrPrimaryPhoto(inboxEntry: InboxEntry, station: Station): Long {
        if (inboxEntry.photoId != null) {
            return inboxEntry.photoId
        }

        val primaryPhoto = station.primaryPhoto
        if (primaryPhoto != null) {
            return station.primaryPhoto!!.id
        }

        throw IllegalArgumentException("Station has no primary photo")
    }

    override fun markProblemReportSolved(command: InboxCommand) {
        val inboxEntry = assertPendingInboxEntryExists(command)
        inboxPort.done(inboxEntry.id)
        log.info("Problem report {} resolved", inboxEntry.id)
    }

    private fun assertStationExists(inboxEntry: InboxEntry): Station {
        return findStationByCountryAndId(inboxEntry.countryCode, inboxEntry.stationId)
            ?: throw IllegalArgumentException("Station not found")
    }

    private fun assertStationExistsAndHasPhoto(inboxEntry: InboxEntry): Station {
        val station = assertStationExists(inboxEntry)
        require(station.hasPhoto) { "Station has no photo" }
        return station
    }

    override fun importMissingStation(command: InboxCommand) {
        val inboxEntry = assertPendingInboxEntryExists(command)
        log.info("Importing photo of missing stations {}, {}", inboxEntry.id, inboxEntry.filename)

        require(!inboxEntry.isProblemReport) { "Can't import a problem report" }

        val station = findOrCreateStation(command)

        if (inboxEntry.isPhotoUpload) {
            importPhoto(command, inboxEntry, station)
        } else {
            log.info("No photo to import for InboxEntry={}", inboxEntry.id)
        }
        inboxPort.updateMissingStationImported(inboxEntry.id, station.key.country, station.key.id, station.title)
    }

    override fun importPhoto(command: InboxCommand) {
        val inboxEntry = assertPendingInboxEntryExists(command)
        log.info("Importing photo {}, {}", inboxEntry.id, inboxEntry.filename)

        require(inboxEntry.isPhotoUpload) { "No photo to import" }

        val station = findStationByCountryAndId(inboxEntry.countryCode, inboxEntry.stationId)
        require(station != null) { "Station not found" }

        importPhoto(command, inboxEntry, station)
        inboxPort.done(inboxEntry.id)
    }

    private fun importPhoto(command: InboxCommand, inboxEntry: InboxEntry, station: Station) {
        if (hasConflict(inboxEntry.id, station)) {
            require(command.conflictResolution!!.solvesPhotoConflict()) { "There is a conflict with another photo" }
            require(!(!station.hasPhoto && command.conflictResolution != InboxCommand.ConflictResolution.IMPORT_AS_NEW_PRIMARY_PHOTO)) {
                "Conflict with another upload! The only possible ConflictResolution strategy is IMPORT_AS_NEW_PRIMARY_PHOTO."
            }
        }

        val photographer = userPort.findById(inboxEntry.photographerId)
        require(photographer != null) { "Photographer ${inboxEntry.photographerId} not found" }
        val country = countryPort.findById(station.key.country.lowercase())
        require(country != null) { "Country ${station.key.country} not found" }

        try {
            val urlPath = photoStoragePort.importPhoto(inboxEntry, station)

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
            val photoId = if (station.hasPhoto) {
                when (command.conflictResolution) {
                    InboxCommand.ConflictResolution.IMPORT_AS_NEW_PRIMARY_PHOTO -> {
                        photoPort.setAllPhotosForStationSecondary(station.key)
                        photoPort.insert(photo.copy(primary = true))
                    }

                    InboxCommand.ConflictResolution.IMPORT_AS_NEW_SECONDARY_PHOTO -> {
                        photoPort.insert(photo)
                    }

                    InboxCommand.ConflictResolution.OVERWRITE_EXISTING_PHOTO -> {
                        val primaryPhoto = station.primaryPhoto
                        require(primaryPhoto != null) { "Station has no primary photo to overwrite" }
                        photoPort.update(photo.copy(id = primaryPhoto.id, primary = true))
                        primaryPhoto.id
                    }

                    else -> throw IllegalArgumentException("No suitable conflict resolution provided")
                }
            } else {
                photoPort.insert(photo.copy(primary = true))
            }

            inboxPort.updatePhotoId(inboxEntry.id, photoId)
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
        val country = countryPort.findById(command.countryCode.lowercase())
        require(country != null) { "Country not found" }
        require(command.stationId.startsWith("Z")) { "Station ID can't be empty and must start with Z" }
        require(!(!command.hasCoords || !command.coordinates!!.isValid)) { "No valid coordinates provided" }
        require(
            !(hasConflict(
                command.id,
                command.coordinates
            ) && !command.conflictResolution!!.solvesStationConflict())
        ) { "There is a conflict with a nearby station" }
        require(!command.title.isNullOrBlank()) { "Station title can't be empty" }
        requireNotNull(command.active) { "No Active flag provided" }

        val newStation = Station(
            key = Station.Key(country.code, "Z${stationPort.maxZ + 1}"),
            title = command.title,
            coordinates = command.coordinates,
            ds100 = command.ds100,
            photos = mutableListOf(),
            active = command.active
        )
        stationPort.insert(newStation)
        log.info("New station '{}' created: {}", newStation.title, newStation.key)
        return newStation
    }

    override fun rejectInboxEntry(command: InboxCommand) {
        val inboxEntry = assertPendingInboxEntryExists(command)
        inboxPort.reject(inboxEntry.id, command.rejectReason!!)
        if (inboxEntry.isProblemReport) {
            log.info("Rejecting problem report {}, {}", inboxEntry.id, command.rejectReason)
            return
        }

        log.info("Rejecting upload {}, {}, {}", inboxEntry.id, command.rejectReason, inboxEntry.filename)

        try {
            photoStoragePort.reject(inboxEntry)
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
            if (stationTitle.isNullOrBlank() || latitude == null || longitude == null) {
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
                countryCode = station?.key?.country ?: countryCode,
                stationId = station?.key?.id,
                title = stationTitle,
                coordinates = coordinates,
                photographerId = user.id,
                extension = extension,
                comment = comment,
                createdAt = Instant.now(),
                active = active,
            )

            id = inboxPort.insert(inboxEntry)
            if (extension != null) {
                filename = createInboxFilename(id, extension)
                crc32 = photoStoragePort.storeUpload(body!!, filename)
                inboxPort.updateCrc32(id, crc32)
                inboxUrl = "$inboxBaseUrl/${UriUtils.encodePath(filename, StandardCharsets.UTF_8)}"
            }

            val duplicateInfo = if (conflict) " (possible duplicate!)" else ""
            val countryCodeParam = countryCode?.let { "countryCode=$countryCode&" } ?: ""
            if (station != null) {
                monitorPort.sendMessage(
                    """
                        New photo upload for ${station.title} - ${station.key.country}:${station.key.id}$duplicateInfo
                        ${comment?.trim() ?: ""}
                        $inboxUrl
                        by ${user.name}
                        via $clientInfo
                    """.trimIndent(),
                    photoStoragePort.getUploadFile(filename!!)
                )
            } else if (filename != null) {
                monitorPort.sendMessage(
                    """
                        Photo upload for missing station $stationTitle$duplicateInfo at https://map.railway-stations.org/index.php?${countryCodeParam}mlat=$latitude&mlon=$longitude&zoom=18&layers=M
                        ${comment?.trim() ?: ""}
                        $inboxUrl
                        by ${user.name}
                        via $clientInfo
                    """.trimIndent(),
                    photoStoragePort.getUploadFile(filename)
                )
            } else {
                monitorPort.sendMessage(
                    """
                        Report missing station $stationTitle$duplicateInfo at https://map.railway-stations.org/index.php?${countryCodeParam}mlat=$latitude&mlon=$longitude&zoom=18&layers=M
                        ${comment?.trim() ?: ""}
                        by ${user.name}
                        via $clientInfo
                    """.trimIndent()
                )
            }
        } catch (e: PhotoTooLargeException) {
            return InboxResponse(
                state = InboxResponse.InboxResponseState.PHOTO_TOO_LARGE,
                message = "Photo too large, max ${e.maxSize} bytes allowed"
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
        if (station.hasPhoto) {
            return true
        }
        return inboxPort.countPendingInboxEntriesForStation(id, station.key.country, station.key.id) > 0
    }

    private fun hasConflict(id: Long?, coordinates: Coordinates?): Boolean {
        if (coordinates == null || coordinates.hasZeroCoords) {
            return false
        }
        return inboxPort.countPendingInboxEntriesForNearbyCoordinates(id, coordinates) > 0
                || stationPort.countNearbyCoordinates(coordinates) > 0
    }

    private fun findStationByCountryAndId(countryCode: String?, stationId: String?): Station? {
        if (countryCode == null || stationId == null) {
            return null
        }
        return stationPort.findByKey(countryCode, stationId)
    }

}

fun getLicenseForPhoto(photographer: User, country: Country) = country.overrideLicense ?: photographer.license
