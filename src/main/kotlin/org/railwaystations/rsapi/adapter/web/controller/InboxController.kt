package org.railwaystations.rsapi.adapter.web.controller

import jakarta.validation.Valid
import org.railwaystations.rsapi.adapter.web.InboxResponseMapper
import org.railwaystations.rsapi.adapter.web.InboxResponseMapper.toHttpStatus
import org.railwaystations.rsapi.adapter.web.RequestUtil
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
import org.railwaystations.rsapi.core.ports.ManageInboxUseCase
import org.railwaystations.rsapi.core.ports.ManageProfileUseCase
import org.railwaystations.rsapi.utils.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
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
) {

    private val log by Logger()

    private fun toDomain(problemReport: ProblemReportDto): ProblemReport {
        return ProblemReport(
            countryCode = problemReport.countryCode,
            stationId = problemReport.stationId,
            title = problemReport.title,
            photoId = problemReport.photoId,
            type = toDomain(problemReport.type),
            comment = problemReport.comment,
            coordinates = mapCoordinates(problemReport.lat, problemReport.lon),
        )
    }

    private fun toDomain(dtoType: ProblemReportTypeDto): ProblemReportType {
        return when (dtoType) {
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
    }

    private fun mapCoordinates(lat: Double?, lon: Double?): Coordinates? {
        if (lat == null || lon == null) {
            return null
        }
        return Coordinates(lat, lon)
    }

    private fun toPublicInboxEntryDto(publicInboxEntries: List<PublicInboxEntry>): List<PublicInboxEntryDto> {
        return publicInboxEntries.map { publicInboxEntry -> toDto(publicInboxEntry) }
    }

    private fun toDto(publicInboxEntry: PublicInboxEntry): PublicInboxEntryDto {
        return PublicInboxEntryDto(
            countryCode = publicInboxEntry.countryCode,
            stationId = publicInboxEntry.stationId,
            title = publicInboxEntry.title,
            lat = publicInboxEntry.lat,
            lon = publicInboxEntry.lon
        )
    }

    private fun toIdList(inboxStateQueryRequestDtos: List<InboxStateQueryRequestDto>): List<Long> {
        return inboxStateQueryRequestDtos.map { it.id }
    }

    private fun toInboxStateQueryDto(inboxStateQueries: List<InboxStateQuery>): List<InboxStateQueryResponseDto> {
        return inboxStateQueries.map { toDto(it) }
    }

    private fun toDto(inboxStateQuery: InboxStateQuery): InboxStateQueryResponseDto {
        return InboxStateQueryResponseDto(
            id = inboxStateQuery.id,
            state = toDto(inboxStateQuery.state),
            countryCode = inboxStateQuery.countryCode,
            stationId = inboxStateQuery.stationId,
            title = inboxStateQuery.title,
            lat = inboxStateQuery.coordinates?.lat,
            lon = inboxStateQuery.coordinates?.lon,
            newTitle = inboxStateQuery.newTitle,
            newLat = inboxStateQuery.newCoordinates?.lat,
            newLon = inboxStateQuery.newCoordinates?.lon,
            comment = inboxStateQuery.comment,
            problemReportType = toDto(inboxStateQuery.problemReportType),
            rejectedReason = inboxStateQuery.rejectedReason,
            filename = inboxStateQuery.filename,
            inboxUrl = inboxStateQuery.inboxUrl,
            crc32 = inboxStateQuery.crc32,
            createdAt = inboxStateQuery.createdAt?.toEpochMilli(),
        )
    }

    private fun toDto(inboxState: InboxState): State {
        return when (inboxState) {
            InboxState.REVIEW, InboxState.CONFLICT -> State.REVIEW
            InboxState.ACCEPTED -> State.ACCEPTED
            InboxState.REJECTED -> State.REJECTED
            InboxState.UNKNOWN -> State.UNKNOWN
        }
    }

    private fun toInboxEntryDto(inboxEntries: List<InboxEntry>): List<InboxEntryDto> {
        return inboxEntries.map { toDto(it) }
    }

    private fun toDto(inboxEntry: InboxEntry): InboxEntryDto {
        return InboxEntryDto(
            id = inboxEntry.id,
            photographerNickname = inboxEntry.photographerNickname!!,
            comment = inboxEntry.comment ?: "",
            createdAt = inboxEntry.createdAt!!.toEpochMilli(),
            done = inboxEntry.done,
            hasPhoto = inboxEntry.hasPhoto(),
            countryCode = inboxEntry.countryCode,
            stationId = inboxEntry.stationId,
            title = inboxEntry.title,
            lat = inboxEntry.lat,
            lon = inboxEntry.lon,
            newTitle = inboxEntry.newTitle,
            newLat = inboxEntry.newLat,
            newLon = inboxEntry.newLon,
            photographerEmail = inboxEntry.photographerEmail,
            photoId = inboxEntry.photoId,
            filename = inboxEntry.filename,
            inboxUrl = inboxEntry.inboxUrl,
            hasConflict = inboxEntry.conflict,
            problemReportType = toDto(inboxEntry.problemReportType),
            isProcessed = inboxEntry.processed,
            active = inboxEntry.active,
        )
    }

    private fun toDto(problemReportType: ProblemReportType?): ProblemReportTypeDto? {
        return when (problemReportType) {
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
    }

    private fun toDomain(command: InboxCommandDto): InboxCommand {
        return InboxCommand(
            id = command.id,
            countryCode = command.countryCode,
            stationId = command.stationId,
            title = command.title,
            coordinates = mapCoordinates(command.lat, command.lon),
            rejectReason = command.rejectReason,
            ds100 = command.DS100,
            active = command.active,
            conflictResolution = toDomain(command.conflictResolution),
        )
    }

    private fun toDomain(conflictResolution: ConflictResolution?): InboxCommand.ConflictResolution {
        return when (conflictResolution) {
            null -> InboxCommand.ConflictResolution.DO_NOTHING
            ConflictResolution.DO_NOTHING -> InboxCommand.ConflictResolution.DO_NOTHING
            ConflictResolution.OVERWRITE_EXISTING_PHOTO -> InboxCommand.ConflictResolution.OVERWRITE_EXISTING_PHOTO
            ConflictResolution.IMPORT_AS_NEW_PRIMARY_PHOTO -> InboxCommand.ConflictResolution.IMPORT_AS_NEW_PRIMARY_PHOTO
            ConflictResolution.IMPORT_AS_NEW_SECONDARY_PHOTO -> InboxCommand.ConflictResolution.IMPORT_AS_NEW_SECONDARY_PHOTO
            ConflictResolution.IGNORE_NEARBY_STATION -> InboxCommand.ConflictResolution.IGNORE_NEARBY_STATION
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/adminInboxCount"],
        produces = ["application/json"]
    )
    fun adminInboxCountGet(): ResponseEntity<InboxCountResponseDto> {
        return ResponseEntity.ok(InboxCountResponseDto(manageInboxUseCase.countPendingInboxEntries()))
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/adminInbox"],
        produces = ["application/json"]
    )
    fun adminInboxGet(
        @RequestHeader(
            required = true,
            value = "Authorization"
        ) authorization: String
    ): ResponseEntity<List<InboxEntryDto>> {
        return ResponseEntity.ok(toInboxEntryDto(manageInboxUseCase.listAdminInbox(requestUtil.authUser.user)))
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(
        method = [RequestMethod.POST],
        value = ["/adminInbox"],
        produces = ["application/json"],
        consumes = ["application/json"]
    )
    fun adminInboxPost(
        @RequestHeader(required = true, value = "Authorization") authorization: String,
        @Valid @RequestBody inboxCommandDto: InboxCommandDto
    ): ResponseEntity<AdminInboxCommandResponseDto> {
        log.info(
            "Executing adminInbox commandDto {} for Nickname: {}",
            inboxCommandDto.command,
            requestUtil.authUser.username
        )
        try {
            val command: InboxCommand = toDomain(inboxCommandDto)
            when (inboxCommandDto.command) {
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
            log.warn("adminInbox commandDto {} failed", inboxCommandDto, e)
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

    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/publicInbox"],
        produces = ["application/json"]
    )
    fun publicInboxGet(): ResponseEntity<List<PublicInboxEntryDto>> {
        return ResponseEntity.ok(toPublicInboxEntryDto(manageInboxUseCase.publicInbox()))
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(
        method = [RequestMethod.POST],
        value = ["/reportProblem"],
        produces = ["application/json"]
    )
    fun reportProblemPost(
        @RequestHeader(required = true, value = "Authorization") authorization: String,
        @Valid @RequestBody problemReport: ProblemReportDto
    ): ResponseEntity<InboxResponseDto> {
        val locale: Locale = localeResolver.resolveLocale(requestUtil.request)
        val user = requestUtil.authUser.user
        if (user.locale != locale) {
            manageProfileUseCase.updateLocale(user, locale)
        }

        val inboxResponse: InboxResponseDto =
            InboxResponseMapper.toDto(
                manageInboxUseCase.reportProblem(
                    toDomain(problemReport),
                    user,
                    requestUtil.userAgent
                )
            )
        return ResponseEntity<InboxResponseDto>(inboxResponse, toHttpStatus(inboxResponse.state))
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/userInbox"],
        produces = ["application/json"]
    )
    fun userInboxGet(
        @RequestHeader(
            required = true,
            value = "Authorization"
        ) authorization: String
    ): ResponseEntity<List<InboxStateQueryResponseDto>> {
        return ResponseEntity.ok(
            toInboxStateQueryDto(
                manageInboxUseCase.userInbox(
                    requestUtil.authUser.user
                )
            )
        )
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(
        method = [RequestMethod.POST],
        value = ["/userInbox"],
        produces = ["application/json"],
        consumes = ["application/json"]
    )
    fun userInboxPost(
        @RequestHeader(required = true, value = "Authorization") authorization: String,
        @Valid @RequestBody uploadStateQueries: List<InboxStateQueryRequestDto>
    ): ResponseEntity<List<InboxStateQueryResponseDto>> {
        return ResponseEntity.ok(
            toInboxStateQueryDto(
                manageInboxUseCase.userInbox(
                    requestUtil.authUser.user,
                    toIdList(uploadStateQueries)
                )
            )
        )
    }
}
