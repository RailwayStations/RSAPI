package org.railwaystations.rsapi.adapter.web.controller

import org.railwaystations.rsapi.adapter.web.InboxResponseMapper
import org.railwaystations.rsapi.adapter.web.InboxResponseMapper.toHttpStatus
import org.railwaystations.rsapi.adapter.web.RequestUtil
import org.railwaystations.rsapi.adapter.web.api.InboxApi
import org.railwaystations.rsapi.adapter.web.model.AdminInboxCommandResponseDto
import org.railwaystations.rsapi.adapter.web.model.InboxCommandDto
import org.railwaystations.rsapi.adapter.web.model.InboxCommandDto.Command
import org.railwaystations.rsapi.adapter.web.model.InboxCommandDto.ConflictResolution
import org.railwaystations.rsapi.adapter.web.model.InboxCountResponseDto
import org.railwaystations.rsapi.adapter.web.model.InboxEntryDto
import org.railwaystations.rsapi.adapter.web.model.InboxResponseDto
import org.railwaystations.rsapi.adapter.web.model.InboxStateQueryRequestDto
import org.railwaystations.rsapi.adapter.web.model.InboxStateQueryResponseDto
import org.railwaystations.rsapi.adapter.web.model.InboxStateQueryResponseDto.State
import org.railwaystations.rsapi.adapter.web.model.ProblemReportDto
import org.railwaystations.rsapi.adapter.web.model.ProblemReportTypeDto
import org.railwaystations.rsapi.adapter.web.model.PublicInboxEntryDto
import org.railwaystations.rsapi.core.model.Coordinates
import org.railwaystations.rsapi.core.model.InboxCommand
import org.railwaystations.rsapi.core.model.InboxEntry
import org.railwaystations.rsapi.core.model.InboxStateQuery
import org.railwaystations.rsapi.core.model.InboxStateQuery.InboxState
import org.railwaystations.rsapi.core.model.ProblemReport
import org.railwaystations.rsapi.core.model.ProblemReportType
import org.railwaystations.rsapi.core.model.PublicInboxEntry
import org.railwaystations.rsapi.core.ports.inbound.ManageInboxUseCase
import org.railwaystations.rsapi.core.ports.inbound.ManageProfileUseCase
import org.railwaystations.rsapi.core.utils.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.LocaleResolver
import java.util.*

@RestController
@Validated
class InboxController(
    private val manageInboxUseCase: ManageInboxUseCase,
    private val manageProfileUseCase: ManageProfileUseCase,
    private val localeResolver: LocaleResolver,
    private val requestUtil: RequestUtil,
) : InboxApi {

    private val log by Logger()


    @PreAuthorize("hasRole('ADMIN')")
    override fun getAdminInboxCount(): ResponseEntity<InboxCountResponseDto> {
        return ResponseEntity.ok(InboxCountResponseDto(manageInboxUseCase.countPendingInboxEntries()))
    }

    @PreAuthorize("hasRole('ADMIN')")
    override fun getAdminInbox(authorization: String): ResponseEntity<List<InboxEntryDto>> {
        return ResponseEntity.ok(manageInboxUseCase.listAdminInbox(requestUtil.authUser.user).map(InboxEntry::toDto))
    }

    @PreAuthorize("hasRole('ADMIN')")
    override fun postAdminInbox(
        authorization: String,
        inboxCommand: InboxCommandDto
    ): ResponseEntity<AdminInboxCommandResponseDto> {
        log.info(
            "Executing adminInbox commandDto {} for Nickname: {}",
            inboxCommand.command,
            requestUtil.authUser.username
        )
        try {
            val command: InboxCommand = inboxCommand.toDomain()
            when (inboxCommand.command) {
                Command.REJECT -> manageInboxUseCase.rejectInboxEntry(command)
                Command.IMPORT_PHOTO -> manageInboxUseCase.importPhoto(command)
                Command.IMPORT_MISSING_STATION -> manageInboxUseCase.importMissingStation(command)
                Command.ACTIVATE_STATION -> manageInboxUseCase.updateStationActiveState(
                    command,
                    true
                )

                Command.DEACTIVATE_STATION -> manageInboxUseCase.updateStationActiveState(
                    command,
                    false
                )

                Command.DELETE_STATION -> manageInboxUseCase.deleteStation(command)
                Command.DELETE_PHOTO -> manageInboxUseCase.deletePhoto(command)
                Command.MARK_SOLVED -> manageInboxUseCase.markProblemReportSolved(command)
                Command.CHANGE_NAME -> manageInboxUseCase.changeStationTitle(command)
                Command.UPDATE_LOCATION -> manageInboxUseCase.updateLocation(command)
                Command.PHOTO_OUTDATED -> manageInboxUseCase.markPhotoOutdated(command)
            }
        } catch (e: IllegalArgumentException) {
            log.warn("adminInbox commandDto {} failed", inboxCommand, e)
            return ResponseEntity<AdminInboxCommandResponseDto>(
                AdminInboxCommandResponseDto(
                    HttpStatus.BAD_REQUEST.value(),
                    e.message ?: e.toString()
                ), HttpStatus.BAD_REQUEST
            )
        }

        return ResponseEntity<AdminInboxCommandResponseDto>(
            AdminInboxCommandResponseDto(HttpStatus.OK.value(), "ok"),
            HttpStatus.OK
        )
    }

    override fun getPublicInbox(): ResponseEntity<List<PublicInboxEntryDto>> {
        return ResponseEntity.ok(manageInboxUseCase.publicInbox().map(PublicInboxEntry::toDto))
    }

    @PreAuthorize("isAuthenticated()")
    override fun postReportProblem(
        authorization: String,
        problemReport: ProblemReportDto
    ): ResponseEntity<InboxResponseDto> {
        val locale: Locale = localeResolver.resolveLocale(requestUtil.request)
        val user = requestUtil.authUser.user
        if (user.locale != locale) {
            manageProfileUseCase.updateLocale(user, locale)
        }

        val inboxResponse: InboxResponseDto =
            InboxResponseMapper.toDto(
                manageInboxUseCase.reportProblem(
                    problemReport.toDomain(),
                    user,
                    requestUtil.userAgent
                )
            )
        return ResponseEntity<InboxResponseDto>(inboxResponse, toHttpStatus(inboxResponse.state))
    }

    @PreAuthorize("isAuthenticated()")
    override fun getUserInbox(
        authorization: String,
        showCompletedEntries: Boolean?
    ): ResponseEntity<List<InboxStateQueryResponseDto>> {
        return ResponseEntity.ok(
            manageInboxUseCase.userInbox(
                user = requestUtil.authUser.user,
                showCompletedEntries = showCompletedEntries ?: false
            ).toDto()
        )
    }

    @PreAuthorize("isAuthenticated()")
    override fun postUserInbox(
        authorization: String,
        uploadStateQueries: List<InboxStateQueryRequestDto>
    ): ResponseEntity<List<InboxStateQueryResponseDto>> {
        return ResponseEntity.ok(
            manageInboxUseCase.userInbox(
                user = requestUtil.authUser.user,
                ids = uploadStateQueries.map { it.id }
            ).toDto()
        )
    }

    @PreAuthorize("isAuthenticated()")
    override fun deleteUserInbox(id: Long): ResponseEntity<Unit> {
        manageInboxUseCase.deleteUserInboxEntry(requestUtil.authUser.user, id)
        return ResponseEntity.noContent().build()
    }

}

private fun ProblemReportDto.toDomain() =
    ProblemReport(
        countryCode = countryCode,
        stationId = stationId,
        title = title,
        photoId = photoId,
        type = type.toDomain(),
        comment = comment,
        coordinates = mapCoordinates(lat, lon),
    )

private fun ProblemReportTypeDto.toDomain() =
    when (this) {
        ProblemReportTypeDto.OTHER -> ProblemReportType.OTHER
        ProblemReportTypeDto.WRONG_NAME -> ProblemReportType.WRONG_NAME
        ProblemReportTypeDto.WRONG_PHOTO -> ProblemReportType.WRONG_PHOTO
        ProblemReportTypeDto.PHOTO_OUTDATED -> ProblemReportType.PHOTO_OUTDATED
        ProblemReportTypeDto.STATION_ACTIVE -> ProblemReportType.STATION_ACTIVE
        ProblemReportTypeDto.STATION_INACTIVE -> ProblemReportType.STATION_INACTIVE
        ProblemReportTypeDto.WRONG_LOCATION -> ProblemReportType.WRONG_LOCATION
        ProblemReportTypeDto.STATION_NONEXISTENT -> ProblemReportType.STATION_NONEXISTENT
        ProblemReportTypeDto.DUPLICATE -> ProblemReportType.DUPLICATE
    }

private fun mapCoordinates(lat: Double?, lon: Double?): Coordinates? {
    if (lat == null || lon == null) {
        return null
    }
    return Coordinates(lat, lon)
}

private fun PublicInboxEntry.toDto() =
    PublicInboxEntryDto(
        countryCode = countryCode,
        stationId = stationId,
        title = title,
        lat = lat,
        lon = lon
    )

private fun List<InboxStateQuery>.toDto(): List<InboxStateQueryResponseDto> =
    map(InboxStateQuery::toDto)

private fun InboxStateQuery.toDto() =
    InboxStateQueryResponseDto(
        id = id,
        state = state.toDto(),
        countryCode = countryCode,
        stationId = stationId,
        title = title,
        lat = coordinates?.lat,
        lon = coordinates?.lon,
        newTitle = newTitle,
        newLat = newCoordinates?.lat,
        newLon = newCoordinates?.lon,
        comment = comment,
        problemReportType = problemReportType.toDto(),
        rejectedReason = rejectedReason,
        filename = filename,
        inboxUrl = inboxUrl,
        crc32 = crc32,
        createdAt = createdAt?.toEpochMilli(),
    )

private fun InboxState.toDto() =
    when (this) {
        InboxState.REVIEW, InboxState.CONFLICT -> State.REVIEW
        InboxState.ACCEPTED -> State.ACCEPTED
        InboxState.REJECTED -> State.REJECTED
        InboxState.UNKNOWN -> State.UNKNOWN
    }

private fun InboxEntry.toDto() =
    InboxEntryDto(
        id = id,
        photographerNickname = photographerNickname!!,
        comment = comment ?: "",
        createdAt = createdAt!!.toEpochMilli(),
        done = done,
        hasPhoto = hasPhoto,
        countryCode = countryCode,
        stationId = stationId,
        title = title,
        lat = lat,
        lon = lon,
        newTitle = newTitle,
        newLat = newLat,
        newLon = newLon,
        photographerEmail = photographerEmail,
        photoId = photoId,
        filename = filename,
        inboxUrl = inboxUrl,
        hasConflict = conflict,
        problemReportType = problemReportType.toDto(),
        isProcessed = processed,
        active = active,
    )

private fun ProblemReportType?.toDto() =
    when (this) {
        ProblemReportType.STATION_NONEXISTENT -> ProblemReportTypeDto.STATION_NONEXISTENT
        ProblemReportType.WRONG_LOCATION -> ProblemReportTypeDto.WRONG_LOCATION
        ProblemReportType.STATION_ACTIVE -> ProblemReportTypeDto.STATION_ACTIVE
        ProblemReportType.STATION_INACTIVE -> ProblemReportTypeDto.STATION_INACTIVE
        ProblemReportType.PHOTO_OUTDATED -> ProblemReportTypeDto.PHOTO_OUTDATED
        ProblemReportType.WRONG_PHOTO -> ProblemReportTypeDto.WRONG_PHOTO
        ProblemReportType.WRONG_NAME -> ProblemReportTypeDto.WRONG_NAME
        ProblemReportType.OTHER -> ProblemReportTypeDto.OTHER
        ProblemReportType.DUPLICATE -> ProblemReportTypeDto.DUPLICATE
        null -> null
    }

private fun InboxCommandDto.toDomain() =
    InboxCommand(
        id = id,
        countryCode = countryCode,
        stationId = stationId,
        title = title,
        coordinates = mapCoordinates(lat, lon),
        rejectReason = rejectReason,
        ds100 = DS100,
        active = active,
        conflictResolution = conflictResolution.toDomain(),
    )

private fun ConflictResolution?.toDomain() =
    when (this) {
        null -> InboxCommand.ConflictResolution.DO_NOTHING
        ConflictResolution.DO_NOTHING -> InboxCommand.ConflictResolution.DO_NOTHING
        ConflictResolution.OVERWRITE_EXISTING_PHOTO -> InboxCommand.ConflictResolution.OVERWRITE_EXISTING_PHOTO
        ConflictResolution.IMPORT_AS_NEW_PRIMARY_PHOTO -> InboxCommand.ConflictResolution.IMPORT_AS_NEW_PRIMARY_PHOTO
        ConflictResolution.IMPORT_AS_NEW_SECONDARY_PHOTO -> InboxCommand.ConflictResolution.IMPORT_AS_NEW_SECONDARY_PHOTO
        ConflictResolution.IGNORE_NEARBY_STATION -> InboxCommand.ConflictResolution.IGNORE_NEARBY_STATION
    }
