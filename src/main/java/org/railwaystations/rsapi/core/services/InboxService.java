package org.railwaystations.rsapi.core.services;

import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.railwaystations.rsapi.adapter.out.db.CountryDao;
import org.railwaystations.rsapi.adapter.out.db.InboxDao;
import org.railwaystations.rsapi.adapter.out.db.PhotoDao;
import org.railwaystations.rsapi.adapter.out.db.StationDao;
import org.railwaystations.rsapi.adapter.out.db.UserDao;
import org.railwaystations.rsapi.core.model.Coordinates;
import org.railwaystations.rsapi.core.model.Country;
import org.railwaystations.rsapi.core.model.InboxCommand;
import org.railwaystations.rsapi.core.model.InboxEntry;
import org.railwaystations.rsapi.core.model.InboxResponse;
import org.railwaystations.rsapi.core.model.InboxStateQuery;
import org.railwaystations.rsapi.core.model.License;
import org.railwaystations.rsapi.core.model.Photo;
import org.railwaystations.rsapi.core.model.ProblemReport;
import org.railwaystations.rsapi.core.model.PublicInboxEntry;
import org.railwaystations.rsapi.core.model.Station;
import org.railwaystations.rsapi.core.model.User;
import org.railwaystations.rsapi.core.ports.in.ManageInboxUseCase;
import org.railwaystations.rsapi.core.ports.out.MastodonBot;
import org.railwaystations.rsapi.core.ports.out.Monitor;
import org.railwaystations.rsapi.core.ports.out.PhotoStorage;
import org.railwaystations.rsapi.utils.ImageUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class InboxService implements ManageInboxUseCase {

    private final PhotoStorage photoStorage;
    private final InboxDao inboxDao;
    private final StationDao stationDao;
    private final UserDao userDao;
    private final CountryDao countryDao;
    private final PhotoDao photoDao;
    private final String inboxBaseUrl;
    private final String photoBaseUrl;
    private final MastodonBot mastodonBot;
    private final Monitor monitor;
    private final Clock clock;

    public InboxService(StationDao stationDao, PhotoStorage photoStorage, Monitor monitor,
                        InboxDao inboxDao, UserDao userDao, CountryDao countryDao,
                        PhotoDao photoDao, @Value("${inboxBaseUrl}") String inboxBaseUrl, MastodonBot mastodonBot,
                        @Value("${photoBaseUrl}") String photoBaseUrl, Clock clock) {
        this.stationDao = stationDao;
        this.photoStorage = photoStorage;
        this.monitor = monitor;
        this.inboxDao = inboxDao;
        this.userDao = userDao;
        this.countryDao = countryDao;
        this.photoDao = photoDao;
        this.inboxBaseUrl = inboxBaseUrl;
        this.mastodonBot = mastodonBot;
        this.photoBaseUrl = photoBaseUrl;
        this.clock = clock;
    }

    @Override
    public InboxResponse reportProblem(ProblemReport problemReport, User user, String clientInfo) {
        if (!user.isEligibleToReportProblem()) {
            log.info("New problem report failed for user {}, profile incomplete", user.getName());
            return InboxResponse.of(InboxResponse.InboxResponseState.UNAUTHORIZED, "Profile incomplete");
        }

        log.info("New problem report: Nickname: {}; Country: {}; Station-Id: {}",
                user.getName(), problemReport.getCountryCode(), problemReport.getStationId());
        var station = findStationByCountryAndId(problemReport.getCountryCode(), problemReport.getStationId());
        if (station.isEmpty()) {
            return InboxResponse.of(InboxResponse.InboxResponseState.NOT_ENOUGH_DATA, "Station not found");
        }
        if (StringUtils.isBlank(problemReport.getComment())) {
            return InboxResponse.of(InboxResponse.InboxResponseState.NOT_ENOUGH_DATA, "Comment is mandatory");
        }
        if (problemReport.getType() == null) {
            return InboxResponse.of(InboxResponse.InboxResponseState.NOT_ENOUGH_DATA, "Problem type is mandatory");
        }
        Long photoId = problemReport.getPhotoId();
        if (problemReport.getType().needsPhoto()) {
            if (!station.get().hasPhoto()) {
                return InboxResponse.of(InboxResponse.InboxResponseState.NOT_ENOUGH_DATA, "Problem type is only applicable to station with photo");
            }
            if (photoId != null) {
                if (station.get().getPhotos().stream().noneMatch(photo -> photo.getId() == problemReport.getPhotoId())) {
                    return InboxResponse.of(InboxResponse.InboxResponseState.NOT_ENOUGH_DATA, "Photo with this id not found at station");
                }
            } else {
                photoId = station.get().getPrimaryPhoto().map(Photo::getId).orElse(null);
            }
        }
        var inboxEntry = InboxEntry.builder()
                .countryCode(problemReport.getCountryCode())
                .stationId(problemReport.getStationId())
                .title(problemReport.getTitle())
                .photoId(photoId)
                .coordinates(problemReport.getCoordinates())
                .photographerId(user.getId())
                .comment(problemReport.getComment())
                .problemReportType(problemReport.getType())
                .createdAt(clock.instant())
                .build();
        monitor.sendMessage(String.format("New problem report for %s - %s:%s%n%s: %s%nby %s%nvia %s",
                station.get().getTitle(), station.get().getKey().getCountry(), station.get().getKey().getId(), problemReport.getType(),
                StringUtils.trimToEmpty(problemReport.getComment()), user.getName(), clientInfo));
        return InboxResponse.of(InboxResponse.InboxResponseState.REVIEW, inboxDao.insert(inboxEntry));
    }

    @Override
    public List<PublicInboxEntry> publicInbox() {
        return inboxDao.findPublicInboxEntries();
    }

    @Override
    public List<InboxStateQuery> userInbox(@NotNull User user) {
        return inboxDao.findByUser(user.getId()).stream()
                .map(this::mapToInboxStateQuery)
                .toList();
    }

    @Override
    public List<InboxStateQuery> userInbox(@NotNull User user, List<Long> ids) {
        return ids.stream()
                .filter(Objects::nonNull)
                .map(inboxDao::findById)
                .filter(inboxEntry -> inboxEntry != null && inboxEntry.getPhotographerId() == user.getId())
                .map(this::mapToInboxStateQuery)
                .toList();
    }

    private InboxStateQuery mapToInboxStateQuery(InboxEntry inboxEntry) {
        inboxEntry.setProcessed(!inboxEntry.isDone() && photoStorage.isProcessed(inboxEntry.getFilename()));
        return InboxStateQuery.builder()
                .id(inboxEntry.getId())
                .state(calculateUserInboxState(inboxEntry))
                .comment(inboxEntry.getComment())
                .problemReportType(inboxEntry.getProblemReportType())
                .rejectedReason(inboxEntry.getRejectReason())
                .countryCode(inboxEntry.getCountryCode())
                .stationId(inboxEntry.getStationId())
                .title(inboxEntry.getTitle())
                .coordinates(inboxEntry.getCoordinates())
                .newTitle(inboxEntry.getNewTitle())
                .newCoordinates(inboxEntry.getNewCoordinates())
                .filename(inboxEntry.getFilename())
                .inboxUrl(getInboxUrl(inboxEntry))
                .crc32(inboxEntry.getCrc32())
                .createdAt(inboxEntry.getCreatedAt())
                .build();
    }

    private InboxStateQuery.InboxState calculateUserInboxState(InboxEntry inboxEntry) {
        if (inboxEntry.isDone()) {
            if (inboxEntry.getRejectReason() == null) {
                return InboxStateQuery.InboxState.ACCEPTED;
            } else {
                return InboxStateQuery.InboxState.REJECTED;
            }
        } else {
            return InboxStateQuery.InboxState.REVIEW;
        }
    }

    @Override
    public List<InboxEntry> listAdminInbox(@NotNull User user) {
        var pendingInboxEntries = inboxDao.findPendingInboxEntries();
        pendingInboxEntries.forEach(this::updateInboxEntry);
        return pendingInboxEntries;
    }

    private void updateInboxEntry(InboxEntry inboxEntry) {
        var filename = inboxEntry.getFilename();
        if (filename != null) {
            inboxEntry.setProcessed(photoStorage.isProcessed(filename));
            inboxEntry.setInboxUrl(getInboxUrl(inboxEntry));
        } else if (inboxEntry.hasPhoto()) {
            inboxEntry.setInboxUrl(photoBaseUrl + inboxEntry.getExistingPhotoUrlPath());
        }
        if (inboxEntry.getStationId() == null && !inboxEntry.getNewCoordinates().hasZeroCoords()) {
            inboxEntry.setConflict(hasConflict(inboxEntry.getId(), inboxEntry.getNewCoordinates()));
        }
    }

    private String getInboxUrl(InboxEntry inboxEntry) {
        if (inboxEntry.getFilename() == null) {
            return null;
        }

        if (inboxEntry.isDone()) {
            if (inboxEntry.getRejectReason() != null) {
                return inboxBaseUrl + "/rejected/" + inboxEntry.getFilename();
            } else {
                return inboxBaseUrl + "/done/" + inboxEntry.getFilename();
            }
        }
        return inboxBaseUrl + (inboxEntry.isProcessed() ? "/processed/" : "/") + inboxEntry.getFilename();
    }

    public void markPhotoOutdated(InboxCommand command) {
        var inboxEntry = assertPendingInboxEntryExists(command);
        var station = assertStationExistsAndHasPhoto(inboxEntry);
        photoDao.updatePhotoOutdated(getPhotoIdFromInboxOrPrimaryPhoto(inboxEntry, station));
        inboxDao.done(inboxEntry.getId());
    }

    public void updateLocation(InboxCommand command) {
        var inboxEntry = assertPendingInboxEntryExists(command);
        var coordinates = command.getCoordinates();
        if (coordinates == null || !coordinates.isValid()) {
            throw new IllegalArgumentException("Can't update location, coordinates: " + command.getCoordinates());
        }

        var station = assertStationExists(inboxEntry);
        stationDao.updateLocation(station.getKey(), coordinates);
        inboxDao.done(inboxEntry.getId());
    }

    @Override
    public long countPendingInboxEntries() {
        return inboxDao.countPendingInboxEntries();
    }

    @Override
    public String getNextZ() {
        return "Z" + (stationDao.getMaxZ() + 1);
    }

    public void updateStationActiveState(InboxCommand command, boolean active) {
        var inboxEntry = assertPendingInboxEntryExists(command);
        var station = assertStationExists(inboxEntry);
        stationDao.updateActive(station.getKey(), active);
        inboxDao.done(inboxEntry.getId());
        log.info("Problem report {} station {} set active to {}", inboxEntry.getId(), station.getKey(), active);
    }

    public void changeStationTitle(InboxCommand command) {
        var inboxEntry = assertPendingInboxEntryExists(command);
        if (StringUtils.isBlank(command.getTitle())) {
            throw new IllegalArgumentException("Empty new title: " + command.getTitle());
        }
        var station = assertStationExists(inboxEntry);
        stationDao.changeStationTitle(station.getKey(), command.getTitle());
        inboxDao.done(inboxEntry.getId());
        log.info("Problem report {} station {} changed name to {}", inboxEntry.getId(), station.getKey(), command.getTitle());
    }

    private InboxEntry assertPendingInboxEntryExists(InboxCommand command) {
        var inboxEntry = inboxDao.findById(command.getId());
        if (inboxEntry == null || inboxEntry.isDone()) {
            throw new IllegalArgumentException("No pending inbox entry found");
        }
        return inboxEntry;
    }

    public void deleteStation(InboxCommand command) {
        var inboxEntry = assertPendingInboxEntryExists(command);
        var station = assertStationExists(inboxEntry);
        stationDao.delete(station.getKey());
        inboxDao.done(inboxEntry.getId());
        log.info("Problem report {} station {} deleted", inboxEntry.getId(), station.getKey());
    }

    public void deletePhoto(InboxCommand command) {
        var inboxEntry = assertPendingInboxEntryExists(command);
        var station = assertStationExistsAndHasPhoto(inboxEntry);
        long photoIdToDelete = getPhotoIdFromInboxOrPrimaryPhoto(inboxEntry, station);
        photoDao.delete(photoIdToDelete);
        if (station.getPrimaryPhoto().map(p -> p.getId() == photoIdToDelete).orElse(false)) {
            station.getPhotos().stream().filter(p -> p.getId() != photoIdToDelete)
                    .findFirst().ifPresent(photo -> photoDao.setPrimary(photo.getId()));
        }
        inboxDao.done(inboxEntry.getId());
        log.info("Problem report {} photo of station {} deleted", inboxEntry.getId(), station.getKey());
    }

    private long getPhotoIdFromInboxOrPrimaryPhoto(InboxEntry inboxEntry, Station station) {
        return inboxEntry.getPhotoId() != null ? inboxEntry.getPhotoId() : station.getPrimaryPhoto().orElseThrow(() -> new IllegalArgumentException("Station has no primary photo")).getId();
    }

    public void markProblemReportSolved(InboxCommand command) {
        var inboxEntry = assertPendingInboxEntryExists(command);
        inboxDao.done(inboxEntry.getId());
        log.info("Problem report {} resolved", inboxEntry.getId());
    }

    private Station assertStationExists(InboxEntry inboxEntry) {
        return findStationByCountryAndId(inboxEntry.getCountryCode(), inboxEntry.getStationId())
                .orElseThrow(() -> new IllegalArgumentException("Station not found"));
    }

    private Station assertStationExistsAndHasPhoto(InboxEntry inboxEntry) {
        var station = assertStationExists(inboxEntry);
        if (!station.hasPhoto()) {
            throw new IllegalArgumentException("Station has no photo");
        }
        return station;
    }

    public void importMissingStation(InboxCommand command) {
        var inboxEntry = assertPendingInboxEntryExists(command);
        log.info("Importing photo {}, {}", inboxEntry.getId(), inboxEntry.getFilename());

        if (inboxEntry.isProblemReport()) {
            throw new IllegalArgumentException("Can't import a problem report");
        }

        var station = findOrCreateStation(command);

        if (inboxEntry.isPhotoUpload()) {
            importPhoto(command, inboxEntry, station);
        } else {
            log.info("No photo to import for InboxEntry={}", inboxEntry.getId());
        }
        inboxDao.done(inboxEntry.getId());
    }

    public void importPhoto(InboxCommand command) {
        var inboxEntry = assertPendingInboxEntryExists(command);
        log.info("Importing photo {}, {}", inboxEntry.getId(), inboxEntry.getFilename());

        if (!inboxEntry.isPhotoUpload()) {
            throw new IllegalArgumentException("No photo to import");
        }

        var station = findStationByCountryAndId(inboxEntry.getCountryCode(), inboxEntry.getStationId())
                .orElseThrow(() -> new IllegalArgumentException("Station not found"));

        importPhoto(command, inboxEntry, station);
        inboxDao.done(inboxEntry.getId());
    }

    private void importPhoto(InboxCommand command, InboxEntry inboxEntry, Station station) {
        if (hasConflict(inboxEntry.getId(), station)) {
            if (!command.getConflictResolution().solvesPhotoConflict()) {
                throw new IllegalArgumentException("There is a conflict with another photo");
            }
            if (!station.hasPhoto() && command.getConflictResolution() != InboxCommand.ConflictResolution.IMPORT_AS_NEW_PRIMARY_PHOTO) {
                throw new IllegalArgumentException("Conflict with another upload! The only possible ConflictResolution strategy is IMPORT_AS_NEW_PRIMARY_PHOTO.");
            }
        }

        var photographer = userDao.findById(inboxEntry.getPhotographerId())
                .orElseThrow(() -> new IllegalArgumentException("Photographer " + inboxEntry.getPhotographerId() + " not found"));
        var country = countryDao.findById(StringUtils.lowerCase(station.getKey().getCountry()))
                .orElseThrow(() -> new IllegalArgumentException("Country " + station.getKey().getCountry() + " not found"));

        try {
            var urlPath = photoStorage.importPhoto(inboxEntry, station);

            var photoBuilder = Photo.builder()
                    .stationKey(station.getKey())
                    .urlPath(urlPath)
                    .photographer(photographer)
                    .createdAt(Instant.now())
                    .license(getLicenseForPhoto(photographer, country));
            Photo photo;
            long photoId;
            if (station.hasPhoto()) {
                switch (command.getConflictResolution()) {
                    case IMPORT_AS_NEW_PRIMARY_PHOTO -> {
                        photoDao.setAllPhotosForStationSecondary(station.getKey());
                        photo = photoBuilder.primary(true).build();
                        photoId = photoDao.insert(photo);
                    }
                    case IMPORT_AS_NEW_SECONDARY_PHOTO -> {
                        photo = photoBuilder.primary(false).build();
                        photoId = photoDao.insert(photo);
                    }
                    case OVERWRITE_EXISTING_PHOTO -> {
                        photoId = station.getPrimaryPhoto()
                                .orElseThrow(() -> new IllegalArgumentException("Station has no primary photo to overwrite"))
                                .getId();
                        photo = photoBuilder
                                .id(photoId)
                                .primary(true)
                                .build();
                        photoDao.update(photo);
                    }
                    default -> throw new IllegalArgumentException("No suitable conflict resolution provided");
                }
            } else {
                photo = photoBuilder.primary(true).build();
                photoId = photoDao.insert(photo);
            }

            log.info("Upload {} with photoId {} accepted: {}", inboxEntry.getId(), photoId, inboxEntry.getFilename());
            mastodonBot.tootNewPhoto(station, inboxEntry, photo, photoId);
        } catch (Exception e) {
            log.error("Error importing upload {} photo {}", inboxEntry.getId(), inboxEntry.getFilename());
            throw new RuntimeException("Error moving file", e);
        }
    }

    /**
     * Gets the applicable license for the given country.
     * We need to override the license for some countries, because of limitations of the "Freedom of panorama".
     */
    protected static License getLicenseForPhoto(User photographer, Country country) {
        if (country != null && country.getOverrideLicense() != null) {
            return country.getOverrideLicense();
        }
        return photographer.getLicense();
    }

    private Station findOrCreateStation(InboxCommand command) {
        var station = findStationByCountryAndId(command.getCountryCode(), command.getStationId());

        return station.orElseGet(() -> {
            // create station
            if (countryDao.findById(StringUtils.lowerCase(command.getCountryCode())).isEmpty()) {
                throw new IllegalArgumentException("Country not found");
            }
            if (StringUtils.isBlank(command.getStationId())) {
                throw new IllegalArgumentException("Station ID can't be empty");
            }
            if (!command.hasCoords() || !command.getCoordinates().isValid()) {
                throw new IllegalArgumentException("No valid coordinates provided");
            }
            if (hasConflict(command.getId(), command.getCoordinates()) && !command.getConflictResolution().solvesStationConflict()) {
                throw new IllegalArgumentException("There is a conflict with a nearby station");
            }
            if (StringUtils.isBlank(command.getTitle())) {
                throw new IllegalArgumentException("Station title can't be empty");
            }
            if (command.getActive() == null) {
                throw new IllegalArgumentException("No Active flag provided");
            }

            var newStation = Station.builder()
                    .key(new Station.Key(command.getCountryCode(), command.getStationId()))
                    .title(command.getTitle())
                    .coordinates(command.getCoordinates())
                    .ds100(command.getDs100())
                    .active(command.getActive())
                    .build();
            stationDao.insert(newStation);
            log.info("New station '{}' created: {}", newStation.getTitle(), newStation.getKey());
            return newStation;
        });
    }

    public void rejectInboxEntry(InboxCommand command) {
        var inboxEntry = assertPendingInboxEntryExists(command);
        inboxDao.reject(inboxEntry.getId(), command.getRejectReason());
        if (inboxEntry.isProblemReport()) {
            log.info("Rejecting problem report {}, {}", inboxEntry.getId(), command.getRejectReason());
            return;
        }

        log.info("Rejecting upload {}, {}, {}", inboxEntry.getId(), command.getRejectReason(), inboxEntry.getFilename());

        try {
            photoStorage.reject(inboxEntry);
        } catch (IOException e) {
            log.warn("Unable to move rejected file {}", inboxEntry.getFilename(), e);
        }
    }

    @Override
    public InboxResponse uploadPhoto(String clientInfo, InputStream body, String stationId,
                                     String countryCode, String contentType, String stationTitle,
                                     Double latitude, Double longitude, String comment,
                                     boolean active, User user) {
        if (!user.isEligibleToUploadPhoto()) {
            log.info("Photo upload failed for user {}, profile incomplete", user.getName());
            return InboxResponse.of(InboxResponse.InboxResponseState.UNAUTHORIZED, "Profile incomplete, not allowed to upload photos");
        }

        var station = findStationByCountryAndId(countryCode, stationId);
        Coordinates coordinates;
        if (station.isEmpty()) {
            log.warn("Station not found");
            if (StringUtils.isBlank(stationTitle) || latitude == null || longitude == null) {
                log.warn("Not enough data for missing station: title={}, latitude={}, longitude={}", stationTitle, latitude, longitude);
                return InboxResponse.of(InboxResponse.InboxResponseState.NOT_ENOUGH_DATA, "Not enough data: either 'countryCode' and 'stationId' or 'title', 'latitude' and 'longitude' have to be provided");
            }
            coordinates = new Coordinates(latitude, longitude);
            if (!coordinates.isValid()) {
                log.warn("Lat/Lon out of range: latitude={}, longitude={}", latitude, longitude);
                return InboxResponse.of(InboxResponse.InboxResponseState.LAT_LON_OUT_OF_RANGE, "'latitude' and/or 'longitude' out of range");
            }
        } else {
            coordinates = null;
        }

        var extension = ImageUtil.mimeToExtension(contentType);
        if (station.isPresent() && extension == null) {
            log.warn("Unknown contentType '{}'", contentType);
            return InboxResponse.of(InboxResponse.InboxResponseState.UNSUPPORTED_CONTENT_TYPE, "unsupported content type (only jpg and png are supported)");
        }

        boolean conflict = hasConflict(null, station.orElse(null)) || hasConflict(null, coordinates);

        String filename = null;
        String inboxUrl = null;
        Long id;
        Long crc32 = null;
        try {
            var inboxEntry = InboxEntry.builder()
                    .countryCode(countryCode)
                    .title(stationTitle)
                    .coordinates(coordinates)
                    .photographerId(user.getId())
                    .extension(extension)
                    .comment(comment)
                    .active(active)
                    .createdAt(Instant.now())
                    .build();

            station.ifPresent(s -> {
                inboxEntry.setCountryCode(s.getKey().getCountry());
                inboxEntry.setStationId(s.getKey().getId());
            });
            id = inboxDao.insert(inboxEntry);
            if (extension != null) {
                filename = InboxEntry.createFilename(id, extension);
                crc32 = photoStorage.storeUpload(body, filename);
                inboxDao.updateCrc32(id, crc32);
                inboxUrl = inboxBaseUrl + "/" + UriUtils.encodePath(filename, StandardCharsets.UTF_8);
            }

            var duplicateInfo = conflict ? " (possible duplicate!)" : "";
            var countryCodeParam = countryCode != null ? "countryCode=" + countryCode + "&" : "";
            if (station.isPresent()) {
                monitor.sendMessage(String.format("New photo upload for %s - %s:%s%n%s%n%s%s%nby %s%nvia %s",
                        station.get().getTitle(), station.get().getKey().getCountry(), station.get().getKey().getId(),
                        StringUtils.trimToEmpty(comment), inboxUrl, duplicateInfo, user.getName(), clientInfo), photoStorage.getUploadFile(filename));
            } else if (filename != null) {
                monitor.sendMessage(String.format("Photo upload for missing station %s at https://map.railway-stations.org/index.php?%smlat=%s&mlon=%s&zoom=18&layers=M%n%s%n%s%s%nby %s%nvia %s",
                        stationTitle, countryCodeParam, latitude, longitude,
                        StringUtils.trimToEmpty(comment), inboxUrl, duplicateInfo, user.getName(), clientInfo), photoStorage.getUploadFile(filename));
            } else {
                monitor.sendMessage(String.format("Report missing station %s at https://map.railway-stations.org/index.php?%smlat=%s&mlon=%s&zoom=18&layers=M%n%s%s%nby %s%nvia %s",
                        stationTitle, countryCodeParam, latitude, longitude,
                        StringUtils.trimToEmpty(comment), duplicateInfo, user.getName(), clientInfo));
            }
        } catch (PhotoStorage.PhotoTooLargeException e) {
            return InboxResponse.of(InboxResponse.InboxResponseState.PHOTO_TOO_LARGE, "Photo too large, max " + e.getMaxSize() + " bytes allowed");
        } catch (IOException e) {
            log.error("Error uploading photo", e);
            return InboxResponse.of(InboxResponse.InboxResponseState.ERROR, "Internal Error");
        }

        return InboxResponse.builder()
                .state(conflict ? InboxResponse.InboxResponseState.CONFLICT : InboxResponse.InboxResponseState.REVIEW)
                .id(id)
                .filename(filename)
                .inboxUrl(inboxUrl)
                .crc32(crc32)
                .build();
    }

    private boolean hasConflict(Long id, Station station) {
        if (station == null) {
            return false;
        }
        if (station.hasPhoto()) {
            return true;
        }
        return inboxDao.countPendingInboxEntriesForStation(id, station.getKey().getCountry(), station.getKey().getId()) > 0;
    }

    private boolean hasConflict(Long id, Coordinates coordinates) {
        if (coordinates == null || coordinates.hasZeroCoords()) {
            return false;
        }
        return inboxDao.countPendingInboxEntriesForNearbyCoordinates(id, coordinates) > 0 || stationDao.countNearbyCoordinates(coordinates) > 0;
    }

    private Optional<Station> findStationByCountryAndId(String countryCode, String stationId) {
        return stationDao.findByKey(countryCode, stationId).stream().findFirst();
    }

}
