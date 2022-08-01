package org.railwaystations.rsapi.core.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.railwaystations.rsapi.adapter.out.db.CountryDao;
import org.railwaystations.rsapi.adapter.out.db.InboxDao;
import org.railwaystations.rsapi.adapter.out.db.PhotoDao;
import org.railwaystations.rsapi.adapter.out.db.StationDao;
import org.railwaystations.rsapi.adapter.out.db.UserDao;
import org.railwaystations.rsapi.core.model.Coordinates;
import org.railwaystations.rsapi.core.model.Country;
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

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
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
    private final MastodonBot mastodonBot;
    private final Monitor monitor;

    public InboxService(StationDao stationDao, PhotoStorage photoStorage, Monitor monitor,
                        InboxDao inboxDao, UserDao userDao, CountryDao countryDao,
                        PhotoDao photoDao, @Value("${inboxBaseUrl}") String inboxBaseUrl, MastodonBot mastodonBot) {
        this.stationDao = stationDao;
        this.photoStorage = photoStorage;
        this.monitor = monitor;
        this.inboxDao = inboxDao;
        this.userDao = userDao;
        this.countryDao = countryDao;
        this.photoDao = photoDao;
        this.inboxBaseUrl = inboxBaseUrl;
        this.mastodonBot = mastodonBot;
    }

    @Override
    public InboxResponse reportProblem(ProblemReport problemReport, User user, String clientInfo) {
        if (!user.isEmailVerified()) {
            log.info("New problem report failed for user {}, email {} not verified", user.getName(), user.getEmail());
            return InboxResponse.of(InboxResponse.InboxResponseState.UNAUTHORIZED, "Email not verified");
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
        if (problemReport.getType().needsPhoto() && !station.get().hasPhoto()) {
            return InboxResponse.of(InboxResponse.InboxResponseState.NOT_ENOUGH_DATA, "Problem type is only applicable to station with photo");
        }
        var inboxEntry = InboxEntry.builder()
                .countryCode(problemReport.getCountryCode())
                .stationId(problemReport.getStationId())
                .coordinates(problemReport.getCoordinates())
                .photographerId(user.getId())
                .comment(problemReport.getComment())
                .problemReportType(problemReport.getType())
                .createdAt(Instant.now())
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
    public List<InboxStateQuery> userInbox(@NotNull User user, List<Long> ids) {
        log.info("Query uploadStatus for Nickname: {}", user.getName());

        return ids.stream()
                .map(inboxDao::findById)
                .filter(inboxEntry -> inboxEntry != null && inboxEntry.getPhotographerId() == user.getId())
                .map(this::mapToInboxStateQuery)
                .toList();
    }

    private InboxStateQuery mapToInboxStateQuery(InboxEntry inboxEntry) {
        return InboxStateQuery.builder()
                .id(inboxEntry.getId())
                .state(calculateInboxState(inboxEntry))
                .rejectedReason(inboxEntry.getRejectReason())
                .countryCode(inboxEntry.getCountryCode())
                .stationId(inboxEntry.getStationId())
                .coordinates(inboxEntry.getCoordinates())
                .filename(inboxEntry.getFilename())
                .inboxUrl(getInboxUrl(inboxEntry.getFilename(), photoStorage.isProcessed(inboxEntry.getFilename())))
                .crc32(inboxEntry.getCrc32())
                .build();
    }

    private InboxStateQuery.InboxState calculateInboxState(InboxEntry inboxEntry) {
        if (inboxEntry.isDone()) {
            if (inboxEntry.getRejectReason() == null) {
                return InboxStateQuery.InboxState.ACCEPTED;
            } else {
                return InboxStateQuery.InboxState.REJECTED;
            }
        } else {
            if (hasConflict(inboxEntry.getId(),
                    findStationByCountryAndId(inboxEntry.getCountryCode(), inboxEntry.getStationId()).orElse(null))
                    || (inboxEntry.getStationId() == null && hasConflict(inboxEntry.getId(), inboxEntry.getCoordinates()))) {
                return InboxStateQuery.InboxState.CONFLICT;
            } else {
                return InboxStateQuery.InboxState.REVIEW;
            }
        }
    }

    @Override
    public List<InboxEntry> listAdminInbox(@NotNull User user) {
        log.info("Load adminInbox for Nickname: {}", user.getName());
        var pendingInboxEntries = inboxDao.findPendingInboxEntries();
        pendingInboxEntries.forEach(this::updateInboxEntry);
        return pendingInboxEntries;
    }

    private void updateInboxEntry(InboxEntry inboxEntry) {
        var filename = inboxEntry.getFilename();
        inboxEntry.setProcessed(photoStorage.isProcessed(filename));
        if (!inboxEntry.isProblemReport()) {
            inboxEntry.setInboxUrl(getInboxUrl(filename, inboxEntry.isProcessed()));
        }
        if (inboxEntry.getStationId() == null && !inboxEntry.getCoordinates().hasZeroCoords()) {
            inboxEntry.setConflict(hasConflict(inboxEntry.getId(), inboxEntry.getCoordinates()));
        }
    }

    private String getInboxUrl(String filename, boolean processed) {
        return inboxBaseUrl + (processed ? "/processed/" : "/") + filename;
    }

    @Override
    public void processAdminInboxCommand(@NotNull User user, @NotNull InboxEntry command) {
        log.info("Executing adminInbox command {} for Nickname: {}", command.getCommand(), user.getName());
        var inboxEntry = inboxDao.findById(command.getId());
        if (inboxEntry == null || inboxEntry.isDone()) {
            throw new IllegalArgumentException("No pending inbox entry found");
        }
        switch (command.getCommand()) {
            case REJECT -> rejectInboxEntry(inboxEntry, command.getRejectReason());
            case IMPORT -> importUpload(inboxEntry, command);
            case ACTIVATE_STATION -> updateStationActiveState(inboxEntry, true);
            case DEACTIVATE_STATION -> updateStationActiveState(inboxEntry, false);
            case DELETE_STATION -> deleteStation(inboxEntry);
            case DELETE_PHOTO -> deletePrimaryPhoto(inboxEntry);
            case MARK_SOLVED -> markProblemReportSolved(inboxEntry);
            case CHANGE_NAME -> {
                if (StringUtils.isBlank(command.getTitle())) {
                    throw new IllegalArgumentException("Empty new title: " + command.getTitle());
                }
                changeStationTitle(inboxEntry, command.getTitle());
            }
            case UPDATE_LOCATION -> updateLocation(inboxEntry, command);
            case PHOTO_OUTDATED -> markPrimaryPhotoOutdated(inboxEntry);
            default -> throw new IllegalArgumentException("Unexpected command value: " + command.getCommand());
        }
    }

    // TODO: extend this function to use photo id
    private void markPrimaryPhotoOutdated(InboxEntry inboxEntry) {
        var station = assertStationExistsAndHasPhoto(inboxEntry);
        photoDao.updatePhotoOutdated(station.getPhoto().getId());
        inboxDao.done(inboxEntry.getId());
    }

    private void updateLocation(InboxEntry inboxEntry, InboxEntry command) {
        var coordinates = inboxEntry.getCoordinates();
        if (command.hasCoords()) {
            coordinates = command.getCoordinates();
        }
        if (coordinates == null || !coordinates.isValid()) {
            throw new IllegalArgumentException("Can't update location, coordinates: " + command.getCommand());
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

    private void updateStationActiveState(InboxEntry inboxEntry, boolean active) {
        var station = assertStationExists(inboxEntry);
        stationDao.updateActive(station.getKey(), active);
        inboxDao.done(inboxEntry.getId());
        log.info("Problem report {} station {} set active to {}", inboxEntry.getId(), station.getKey(), active);
    }

    private void changeStationTitle(InboxEntry inboxEntry, String newTitle) {
        var station = assertStationExists(inboxEntry);
        stationDao.changeStationTitle(station.getKey(), newTitle);
        inboxDao.done(inboxEntry.getId());
        log.info("Problem report {} station {} change name to {}", inboxEntry.getId(), station.getKey(), newTitle);
    }

    private void deleteStation(InboxEntry inboxEntry) {
        var station = assertStationExists(inboxEntry);
        stationDao.delete(station.getKey());
        inboxDao.done(inboxEntry.getId());
        log.info("Problem report {} station {} deleted", inboxEntry.getId(), station.getKey());
    }

    // TODO: extend this function to delete photo by id
    private void deletePrimaryPhoto(InboxEntry inboxEntry) {
        var station = assertStationExistsAndHasPhoto(inboxEntry);
        photoDao.delete(station.getPhoto().getId());
        inboxDao.done(inboxEntry.getId());
        log.info("Problem report {} photo of station {} deleted", inboxEntry.getId(), station.getKey());
    }

    private void markProblemReportSolved(InboxEntry inboxEntry) {
        assertStationExists(inboxEntry);
        inboxDao.done(inboxEntry.getId());
        log.info("Problem report {} accepted", inboxEntry.getId());
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

    private void importUpload(InboxEntry inboxEntry, InboxEntry command) {
        log.info("Importing upload {}, {}", inboxEntry.getId(), inboxEntry.getFilename());

        if (inboxEntry.isProblemReport()) {
            throw new IllegalArgumentException("Can't import a problem report");
        }

        var station = findOrCreateStation(inboxEntry, command);

        if (station.hasPhoto() && !command.getConflictResolution().solvesPhotoConflict()) {
            throw new IllegalArgumentException("Station already has a photo");
        }
        if (hasConflict(inboxEntry.getId(), station) && !command.getConflictResolution().solvesPhotoConflict()) {
            throw new IllegalArgumentException("There is a conflict with another upload");
        }

        var photographer = userDao.findById(inboxEntry.getPhotographerId()).orElseThrow();
        var country = countryDao.findById(StringUtils.lowerCase(station.getKey().getCountry()))
                .orElseThrow(() -> new IllegalArgumentException("Country not found"));

        try {
            var photoBuilder = Photo.builder()
                    .stationKey(station.getKey())
                    .urlPath(getPhotoUrlPath(inboxEntry, station))
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
                        photoId = station.getPhoto().getId();
                        photo = photoBuilder
                                .id(photoId)
                                .primary(station.getPhoto().isPrimary())
                                .build();
                        photoDao.update(photo);
                    }
                    default -> throw new IllegalArgumentException("No suitable conflict resolution provided");
                }
            } else {
                photo = photoBuilder.primary(true).build();
                photoId = photoDao.insert(photo);
            }

            // TODO: importPhoto needs the photoId
            photoStorage.importPhoto(inboxEntry, station);
            inboxDao.done(inboxEntry.getId());
            log.info("Upload {} with photoId {} accepted: {}", inboxEntry.getId(), photoId, inboxEntry.getFilename());
            // TODO: tootNewPhoto needs the photoId
            mastodonBot.tootNewPhoto(station, inboxEntry, photo);
        } catch (Exception e) {
            log.error("Error importing upload {} photo {}", inboxEntry.getId(), inboxEntry.getFilename());
            throw new RuntimeException("Error moving file", e);
        }
    }

    private String getPhotoUrlPath(InboxEntry inboxEntry, Station station) {
        // TODO: add photoId and extract code together with PhotoFileStorage.java#45
        return "/" + station.getKey().getCountry() + "/" + station.getKey().getId() + "." + inboxEntry.getExtension();
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

    private Station findOrCreateStation(InboxEntry inboxEntry, InboxEntry command) {
        var station = findStationByCountryAndId(inboxEntry.getCountryCode(), inboxEntry.getStationId());
        if (station.isEmpty() && command.getCreateStation()) {
            station = findStationByCountryAndId(command.getCountryCode(), command.getStationId());
            station.ifPresent(s -> log.info("Importing missing station upload {} to existing station {}", inboxEntry.getId(), s.getKey()));
        }

        return station.orElseGet(()-> {
            if (!command.getCreateStation() || StringUtils.isNotBlank(inboxEntry.getStationId())) {
                throw new IllegalArgumentException("Station not found");
            }

            // create station
            var country = countryDao.findById(StringUtils.lowerCase(command.getCountryCode()));
            if (country.isEmpty()) {
                throw new IllegalArgumentException("Country not found");
            }
            if (StringUtils.isBlank(command.getStationId())) {
                throw new IllegalArgumentException("Station ID can't be empty");
            }
            if (hasConflict(inboxEntry.getId(), inboxEntry.getCoordinates()) && !command.getConflictResolution().solvesStationConflict()) {
                throw new IllegalArgumentException("There is a conflict with a nearby station");
            }
            if (command.hasCoords() && !command.getCoordinates().isValid()) {
                throw new IllegalArgumentException("Lat/Lon out of range");
            }

            var coordinates = inboxEntry.getCoordinates();
            if (command.hasCoords()) {
                coordinates = command.getCoordinates();
            }

            var title = command.getTitle() != null ? command.getTitle() : inboxEntry.getTitle();

            var newStation = Station.builder()
                    .key(new Station.Key(command.getCountryCode(), command.getStationId()))
                    .title(title)
                    .coordinates(coordinates)
                    .ds100(command.getDs100())
                    .active(command.getActive())
                    .build();
            stationDao.insert(newStation);
            return newStation;
        });
    }

    private void rejectInboxEntry(InboxEntry inboxEntry, String rejectReason) {
        inboxDao.reject(inboxEntry.getId(), rejectReason);
        if (inboxEntry.isProblemReport()) {
            log.info("Rejecting problem report {}, {}", inboxEntry.getId(), rejectReason);
            return;
        }

        log.info("Rejecting upload {}, {}, {}", inboxEntry.getId(), rejectReason, inboxEntry.getFilename());

        try {
            photoStorage.reject(inboxEntry);
        } catch (IOException e) {
            log.warn("Unable to move rejected file {}", inboxEntry.getFilename(), e);
        }
    }

    @Override
    public InboxResponse uploadPhoto(String clientInfo, InputStream body, String stationId,
                                     String country, String contentType, String stationTitle,
                                     Double latitude, Double longitude, String comment,
                                     boolean active, User user) {
        if (!user.isEmailVerified()) {
            log.info("Photo upload failed for user {}, email not verified", user.getName());
            return InboxResponse.of(InboxResponse.InboxResponseState.UNAUTHORIZED,"Email not verified");
        }

        var station = findStationByCountryAndId(country, stationId);
        Coordinates coordinates;
        if (station.isEmpty()) {
            log.warn("Station not found");
            if (StringUtils.isBlank(stationTitle) || latitude == null || longitude == null) {
                log.warn("Not enough data for missing station: title={}, latitude={}, longitude={}", stationTitle, latitude, longitude);
                return InboxResponse.of(InboxResponse.InboxResponseState.NOT_ENOUGH_DATA, "Not enough data: either 'country' and 'stationId' or 'title', 'latitude' and 'longitude' have to be provided");
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
        if (extension == null) {
            log.warn("Unknown contentType '{}'", contentType);
            return InboxResponse.of(InboxResponse.InboxResponseState.UNSUPPORTED_CONTENT_TYPE, "unsupported content type (only jpg and png are supported)");
        }

        boolean conflict = hasConflict(null, station.orElse(null)) || hasConflict(null, coordinates);

        String filename;
        String inboxUrl;
        Long id;
        Long crc32;
        try {
            var inboxEntry = InboxEntry.builder()
                    .countryCode(country)
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
            filename = InboxEntry.createFilename(id, extension);
            crc32 = photoStorage.storeUpload(body, filename);
            inboxDao.updateCrc32(id, crc32);

            var duplicateInfo = conflict ? " (possible duplicate!)" : "";
            inboxUrl = inboxBaseUrl + "/" + UriUtils.encodePath(filename, StandardCharsets.UTF_8);
            if (station.isPresent()) {
                monitor.sendMessage(String.format("New photo upload for %s - %s:%s%n%s%n%s%s%nby %s%nvia %s",
                        station.get().getTitle(), station.get().getKey().getCountry(), station.get().getKey().getId(),
                        StringUtils.trimToEmpty(comment), inboxUrl, duplicateInfo, user.getName(), clientInfo), photoStorage.getUploadFile(filename));
            } else {
                monitor.sendMessage(String.format("Photo upload for missing station %s at https://map.railway-stations.org/index.php?mlat=%s&mlon=%s&zoom=18&layers=M%n%s%n%s%s%nby %s%nvia %s",
                        stationTitle, latitude, longitude,
                        StringUtils.trimToEmpty(comment), inboxUrl, duplicateInfo, user.getName(), clientInfo), photoStorage.getUploadFile(filename));
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
