package org.railwaystations.rsapi.adapter.in.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.railwaystations.rsapi.adapter.in.web.InboxResponseMapper;
import org.railwaystations.rsapi.adapter.in.web.api.InboxApi;
import org.railwaystations.rsapi.adapter.in.web.model.AdminInboxCommandResponseDto;
import org.railwaystations.rsapi.adapter.in.web.model.InboxCommandDto;
import org.railwaystations.rsapi.adapter.in.web.model.InboxCountResponseDto;
import org.railwaystations.rsapi.adapter.in.web.model.InboxEntryDto;
import org.railwaystations.rsapi.adapter.in.web.model.InboxResponseDto;
import org.railwaystations.rsapi.adapter.in.web.model.InboxStateQueryRequestDto;
import org.railwaystations.rsapi.adapter.in.web.model.InboxStateQueryResponseDto;
import org.railwaystations.rsapi.adapter.in.web.model.NextZResponseDto;
import org.railwaystations.rsapi.adapter.in.web.model.ProblemReportDto;
import org.railwaystations.rsapi.adapter.in.web.model.PublicInboxEntryDto;
import org.railwaystations.rsapi.core.model.Coordinates;
import org.railwaystations.rsapi.core.model.InboxCommand;
import org.railwaystations.rsapi.core.model.InboxEntry;
import org.railwaystations.rsapi.core.model.InboxStateQuery;
import org.railwaystations.rsapi.core.model.ProblemReport;
import org.railwaystations.rsapi.core.model.ProblemReportType;
import org.railwaystations.rsapi.core.model.PublicInboxEntry;
import org.railwaystations.rsapi.core.ports.in.ManageInboxUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.railwaystations.rsapi.adapter.in.web.InboxResponseMapper.toHttpStatus;
import static org.railwaystations.rsapi.adapter.in.web.RequestUtil.getAuthUser;
import static org.railwaystations.rsapi.adapter.in.web.RequestUtil.getUserAgent;

@RestController
@Slf4j
@Validated
@RequiredArgsConstructor
public class InboxController implements InboxApi {

    private final ManageInboxUseCase manageInboxUseCase;

    private ProblemReport toDomain(ProblemReportDto problemReport) {
        return ProblemReport.builder()
                .countryCode(problemReport.getCountryCode())
                .stationId(problemReport.getStationId())
                .photoId(problemReport.getPhotoId())
                .type(toDomain(problemReport.getType()))
                .comment(problemReport.getComment())
                .coordinates(mapCoordinates(problemReport.getLat(), problemReport.getLon()))
                .build();
    }

    private ProblemReportType toDomain(ProblemReportDto.TypeEnum dtoType) {
        return switch (dtoType) {
            case OTHER -> ProblemReportType.OTHER;
            case WRONG_NAME -> ProblemReportType.WRONG_NAME;
            case WRONG_PHOTO -> ProblemReportType.WRONG_PHOTO;
            case PHOTO_OUTDATED -> ProblemReportType.PHOTO_OUTDATED;
            case STATION_ACTIVE -> ProblemReportType.STATION_ACTIVE;
            case STATION_INACTIVE -> ProblemReportType.STATION_INACTIVE;
            case WRONG_LOCATION -> ProblemReportType.WRONG_LOCATION;
            case STATION_NONEXISTENT -> ProblemReportType.STATION_NONEXISTENT;
        };
    }

    private Coordinates mapCoordinates(Double lat, Double lon) {
        if (lat == null || lon == null) {
            return null;
        }
        return new Coordinates(lat, lon);
    }

    private List<PublicInboxEntryDto> toPublicInboxEntryDto(List<PublicInboxEntry> publicInboxEntries) {
        return publicInboxEntries.stream()
                .map(this::toDto)
                .toList();
    }

    private PublicInboxEntryDto toDto(PublicInboxEntry publicInboxEntry) {
        return new PublicInboxEntryDto(publicInboxEntry.getCountryCode(),
                publicInboxEntry.getStationId(),
                publicInboxEntry.getTitle(),
                publicInboxEntry.getLat(),
                publicInboxEntry.getLon());
    }

    private List<Long> toIdList(List<InboxStateQueryRequestDto> inboxStateQueryRequestDtos) {
        return inboxStateQueryRequestDtos.stream()
                .map(InboxStateQueryRequestDto::getId)
                .toList();
    }

    private List<InboxStateQueryResponseDto> toInboxStateQueryDto(List<InboxStateQuery> inboxStateQueries) {
        return inboxStateQueries.stream()
                .map(this::toDto)
                .toList();
    }

    private InboxStateQueryResponseDto toDto(InboxStateQuery inboxStateQuery) {
        return new InboxStateQueryResponseDto(inboxStateQuery.getId(), toDto(inboxStateQuery.getState()))
                .countryCode(inboxStateQuery.getCountryCode())
                .stationId(inboxStateQuery.getStationId())
                .inboxUrl(inboxStateQuery.getInboxUrl())
                .lat(inboxStateQuery.getCoordinates() != null ? inboxStateQuery.getCoordinates().getLat() : null)
                .lon(inboxStateQuery.getCoordinates() != null ? inboxStateQuery.getCoordinates().getLon() : null)
                .filename(inboxStateQuery.getFilename())
                .crc32(inboxStateQuery.getCrc32())
                .rejectedReason(inboxStateQuery.getRejectedReason());
    }

    public InboxStateQueryResponseDto.StateEnum toDto(InboxStateQuery.InboxState inboxState) {
        return switch (inboxState) {
            case REVIEW -> InboxStateQueryResponseDto.StateEnum.REVIEW;
            case ACCEPTED -> InboxStateQueryResponseDto.StateEnum.ACCEPTED;
            case CONFLICT -> InboxStateQueryResponseDto.StateEnum.CONFLICT;
            case REJECTED -> InboxStateQueryResponseDto.StateEnum.REJECTED;
            case UNKNOWN -> InboxStateQueryResponseDto.StateEnum.UNKNOWN;
        };
    }

    private List<InboxEntryDto> toInboxEntryDto(List<InboxEntry> inboxEntries) {
        return inboxEntries.stream()
                .map(this::toDto)
                .toList();
    }

    private InboxEntryDto toDto(InboxEntry inboxEntry) {
        return new InboxEntryDto(inboxEntry.getId(),
                inboxEntry.getPhotographerNickname(),
                inboxEntry.getComment(),
                inboxEntry.getCreatedAt().toEpochMilli(),
                inboxEntry.isDone(),
                inboxEntry.isHasPhoto())
                .countryCode(inboxEntry.getCountryCode())
                .stationId(inboxEntry.getStationId())
                .title(inboxEntry.getTitle())
                .lat(inboxEntry.getLat())
                .lon(inboxEntry.getLon())
                .active(inboxEntry.getActive())
                .inboxUrl(inboxEntry.getInboxUrl())
                .filename(inboxEntry.getFilename())
                .hasConflict(inboxEntry.isConflict())
                .isProcessed(inboxEntry.isProcessed())
                .photographerEmail(inboxEntry.getPhotographerEmail())
                .problemReportType(toDto(inboxEntry.getProblemReportType()));
    }

    private InboxEntryDto.ProblemReportTypeEnum toDto(ProblemReportType problemReportType) {
        if (problemReportType == null) {
            return null;
        }
        return switch (problemReportType) {
            case STATION_NONEXISTENT -> InboxEntryDto.ProblemReportTypeEnum.STATION_NONEXISTENT;
            case WRONG_LOCATION -> InboxEntryDto.ProblemReportTypeEnum.WRONG_LOCATION;
            case STATION_ACTIVE -> InboxEntryDto.ProblemReportTypeEnum.STATION_ACTIVE;
            case STATION_INACTIVE -> InboxEntryDto.ProblemReportTypeEnum.STATION_INACTIVE;
            case PHOTO_OUTDATED -> InboxEntryDto.ProblemReportTypeEnum.PHOTO_OUTDATED;
            case WRONG_PHOTO -> InboxEntryDto.ProblemReportTypeEnum.WRONG_PHOTO;
            case WRONG_NAME -> InboxEntryDto.ProblemReportTypeEnum.WRONG_NAME;
            case OTHER -> InboxEntryDto.ProblemReportTypeEnum.OTHER;
        };
    }

    private InboxCommand toDomain(InboxCommandDto command) {
        return InboxCommand.builder()
                .id(command.getId())
                .countryCode(command.getCountryCode())
                .stationId(command.getStationId())
                .title(command.getTitle())
                .coordinates(mapCoordinates(command.getLat(), command.getLon()))
                .rejectReason(command.getRejectReason())
                .ds100(command.getDS100())
                .active(command.getActive() != null ? command.getActive() : null)
                .conflictResolution(toDomain(command.getConflictResolution()))
                .build();
    }

    private InboxCommand.ConflictResolution toDomain(InboxCommandDto.ConflictResolutionEnum conflictResolution) {
        if (conflictResolution == null) {
            return InboxCommand.ConflictResolution.DO_NOTHING;
        }
        return switch (conflictResolution) {
            case DO_NOTHING -> InboxCommand.ConflictResolution.DO_NOTHING;
            case OVERWRITE_EXISTING_PHOTO -> InboxCommand.ConflictResolution.OVERWRITE_EXISTING_PHOTO;
            case IMPORT_AS_NEW_PRIMARY_PHOTO -> InboxCommand.ConflictResolution.IMPORT_AS_NEW_PRIMARY_PHOTO;
            case IMPORT_AS_NEW_SECONDARY_PHOTO -> InboxCommand.ConflictResolution.IMPORT_AS_NEW_SECONDARY_PHOTO;
            case IGNORE_NEARBY_STATION -> InboxCommand.ConflictResolution.IGNORE_NEARBY_STATION;
        };
    }


    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<InboxCountResponseDto> adminInboxCountGet() {
        return ResponseEntity.ok(new InboxCountResponseDto(manageInboxUseCase.countPendingInboxEntries()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<List<InboxEntryDto>> adminInboxGet(String authorization) {
        return ResponseEntity.ok(toInboxEntryDto(manageInboxUseCase.listAdminInbox(getAuthUser().getUser())));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<AdminInboxCommandResponseDto> adminInboxPost(InboxCommandDto uploadCommand, String authorization) {
        log.info("Executing adminInbox commandDto {} for Nickname: {}", uploadCommand.getCommand(), getAuthUser().getUsername());
        try {
            var command = toDomain(uploadCommand);
            switch (uploadCommand.getCommand()) {
                case REJECT -> manageInboxUseCase.rejectInboxEntry(command);
                case IMPORT_PHOTO -> manageInboxUseCase.importPhoto(command);
                case IMPORT_MISSING_STATION -> manageInboxUseCase.importMissingStation(command);
                case ACTIVATE_STATION -> manageInboxUseCase.updateStationActiveState(command, true);
                case DEACTIVATE_STATION -> manageInboxUseCase.updateStationActiveState(command, false);
                case DELETE_STATION -> manageInboxUseCase.deleteStation(command);
                case DELETE_PHOTO -> manageInboxUseCase.deletePhoto(command);
                case MARK_SOLVED -> manageInboxUseCase.markProblemReportSolved(command);
                case CHANGE_NAME -> manageInboxUseCase.changeStationTitle(command);
                case UPDATE_LOCATION -> manageInboxUseCase.updateLocation(command);
                case PHOTO_OUTDATED -> manageInboxUseCase.markPhotoOutdated(command);
                default ->
                        throw new IllegalArgumentException("Unexpected commandDto value: " + uploadCommand.getCommand());
            }
        } catch (IllegalArgumentException e) {
            log.warn("adminInbox commandDto {} failed", uploadCommand, e);
            return new ResponseEntity<>(new AdminInboxCommandResponseDto(HttpStatus.BAD_REQUEST.value(), e.getMessage()), HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(new AdminInboxCommandResponseDto(HttpStatus.OK.value(), "ok"), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<NextZResponseDto> nextZGet() {
        return ResponseEntity.ok(new NextZResponseDto(manageInboxUseCase.getNextZ()));
    }

    @Override
    public ResponseEntity<List<PublicInboxEntryDto>> publicInboxGet() {
        return ResponseEntity.ok(toPublicInboxEntryDto(manageInboxUseCase.publicInbox()));
    }

    @PreAuthorize("isAuthenticated()")
    @Override
    public ResponseEntity<InboxResponseDto> reportProblemPost(ProblemReportDto problemReport, String authorization) {
        InboxResponseDto inboxResponse = InboxResponseMapper.toDto(manageInboxUseCase.reportProblem(toDomain(problemReport), getAuthUser().getUser(), getUserAgent()));
        return new ResponseEntity<>(inboxResponse, toHttpStatus(inboxResponse.getState()));
    }

    @PreAuthorize("isAuthenticated()")
    @Override
    public ResponseEntity<List<InboxStateQueryResponseDto>> userInboxPost(List<InboxStateQueryRequestDto> uploadStateQueries, String authorization) {
        return ResponseEntity.ok(toInboxStateQueryDto(manageInboxUseCase.userInbox(getAuthUser().getUser(), toIdList(uploadStateQueries))));
    }

}
