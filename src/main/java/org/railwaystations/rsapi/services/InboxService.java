package org.railwaystations.rsapi.services;

import org.apache.commons.lang3.StringUtils;
import org.railwaystations.rsapi.adapter.db.CountryDao;
import org.railwaystations.rsapi.adapter.db.InboxDao;
import org.railwaystations.rsapi.adapter.db.PhotoDao;
import org.railwaystations.rsapi.adapter.web.auth.RSUserDetailsService;
import org.railwaystations.rsapi.domain.model.*;
import org.railwaystations.rsapi.domain.port.out.MastodonBot;
import org.railwaystations.rsapi.domain.port.out.Monitor;
import org.railwaystations.rsapi.domain.port.out.PhotoStorage;
import org.railwaystations.rsapi.utils.ImageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Service
public class InboxService {

    private static final Logger LOG = LoggerFactory.getLogger(InboxService.class);

    private final PhotoStationsService photoStationsService;
    private final PhotoStorage photoStorage;
    private final InboxDao inboxDao;
    private final RSUserDetailsService userDetailsService;
    private final CountryDao countryDao;
    private final PhotoDao photoDao;
    private final String inboxBaseUrl;
    private final MastodonBot mastodonBot;
    private final Monitor monitor;

    public InboxService(final PhotoStationsService photoStationsService, final PhotoStorage photoStorage, final Monitor monitor,
                        final InboxDao inboxDao, final RSUserDetailsService userDetailsService, final CountryDao countryDao,
                        final PhotoDao photoDao, @Value("${inboxBaseUrl}") final String inboxBaseUrl, final MastodonBot mastodonBot) {
        this.photoStationsService = photoStationsService;
        this.photoStorage = photoStorage;
        this.monitor = monitor;
        this.inboxDao = inboxDao;
        this.userDetailsService = userDetailsService;
        this.countryDao = countryDao;
        this.photoDao = photoDao;
        this.inboxBaseUrl = inboxBaseUrl;
        this.mastodonBot = mastodonBot;
    }

    public InboxResponse reportProblem(final ProblemReport problemReport, final User user, final String clientInfo) {
        if (!user.isEmailVerified()) {
            LOG.info("New problem report failed for user {}, email {} not verified", user.getName(), user.getEmail());
            return new InboxResponse(InboxResponse.InboxResponseState.UNAUTHORIZED, "Email not verified");
        }

        LOG.info("New problem report: Nickname: {}; Country: {}; Station-Id: {}",
                user.getName(), problemReport.getCountryCode(), problemReport.getStationId());
        final Optional<Station> station = photoStationsService.findByCountryAndId(problemReport.getCountryCode(), problemReport.getStationId());
        if (station.isEmpty()) {
            return new InboxResponse(InboxResponse.InboxResponseState.NOT_ENOUGH_DATA, "Station not found");
        }
        if (StringUtils.isBlank(problemReport.getComment())) {
            return new InboxResponse(InboxResponse.InboxResponseState.NOT_ENOUGH_DATA, "Comment is mandatory");
        }
        if (problemReport.getType() == null) {
            return new InboxResponse(InboxResponse.InboxResponseState.NOT_ENOUGH_DATA, "Problem type is mandatory");
        }
        final InboxEntry inboxEntry = new InboxEntry(problemReport.getCountryCode(), problemReport.getStationId(),
                null, problemReport.getCoordinates(), user.getId(), null, problemReport.getComment(),
                problemReport.getType(), null);
        monitor.sendMessage(String.format("New problem report for %s - %s:%s%n%s: %s%nby %s%nvia %s",
                station.get().getTitle(), station.get().getKey().getCountry(), station.get().getKey().getId(), problemReport.getType(),
                StringUtils.trimToEmpty(problemReport.getComment()), user.getName(), clientInfo));
        return new InboxResponse(InboxResponse.InboxResponseState.REVIEW, inboxDao.insert(inboxEntry));
    }

    public List<PublicInboxEntry> publicInbox() {
        return inboxDao.findPublicInboxEntries();
    }

    public List<InboxStateQuery> userInbox(@NotNull final User user, @NotNull final List<InboxStateQuery> queries) {
        LOG.info("Query uploadStatus for Nickname: {}", user.getName());

        for (final InboxStateQuery query : queries) {
            query.setState(InboxStateQuery.InboxState.UNKNOWN);
            InboxEntry inboxEntry = null;
            if (query.getId() != null) {
                inboxEntry = inboxDao.findById(query.getId());
            } else if (query.getCountryCode() != null && query.getStationId() != null) {
                inboxEntry = inboxDao.findNewestPendingByCountryAndStationIdAndPhotographerId(query.getCountryCode(), query.getStationId(), user.getId());
            }

            if (inboxEntry != null && inboxEntry.getPhotographerId() == user.getId()) {
                query.setId(inboxEntry.getId());
                query.setRejectedReason(inboxEntry.getRejectReason());
                query.setCountryCode(inboxEntry.getCountryCode());
                query.setStationId(inboxEntry.getStationId());
                query.setCoordinates(inboxEntry.getCoordinates());
                query.setFilename(inboxEntry.getFilename());
                query.setInboxUrl(getInboxUrl(inboxEntry.getFilename(), photoStorage.isProcessed(inboxEntry.getFilename())));
                query.setCrc32(inboxEntry.getCrc32());


                if (inboxEntry.isDone()) {
                    if (inboxEntry.getRejectReason() == null) {
                        query.setState(InboxStateQuery.InboxState.ACCEPTED);
                    } else {
                        query.setState(InboxStateQuery.InboxState.REJECTED);
                    }
                } else {
                    if (hasConflict(inboxEntry.getId(),
                            photoStationsService.findByCountryAndId(query.getCountryCode(), query.getStationId()).orElse(null))
                            || (inboxEntry.getStationId() == null && hasConflict(inboxEntry.getId(), inboxEntry.getCoordinates()))) {
                        query.setState(InboxStateQuery.InboxState.CONFLICT);
                    } else {
                        query.setState(InboxStateQuery.InboxState.REVIEW);
                    }
                }
            }
        }

        return queries;
    }

    public List<InboxEntry> listAdminInbox(@NotNull final User user) {
        LOG.info("Load adminInbox for Nickname: {}", user.getName());
        final List<InboxEntry> pendingInboxEntries = inboxDao.findPendingInboxEntries();
        for (final InboxEntry inboxEntry : pendingInboxEntries) {
            final String filename = inboxEntry.getFilename();
            inboxEntry.isProcessed(photoStorage.isProcessed(filename));
            if (!inboxEntry.isProblemReport()) {
                inboxEntry.setInboxUrl(getInboxUrl(filename, inboxEntry.isProcessed()));
            }
            if (inboxEntry.getStationId() == null && !inboxEntry.getCoordinates().hasZeroCoords()) {
                inboxEntry.setConflict(hasConflict(inboxEntry.getId(), inboxEntry.getCoordinates()));
            }
        }
        return pendingInboxEntries;
    }

    private String getInboxUrl(final String filename, final boolean processed) {
        return inboxBaseUrl + (processed ? "/processed/" : "/") + filename;
    }

    public void processAdminInboxCommand(@NotNull final User user, @NotNull final InboxEntry command) {
        LOG.info("Executing adminInbox command {} for Nickname: {}", command.getCommand(), user.getName());
        final InboxEntry inboxEntry = inboxDao.findById(command.getId());
        if (inboxEntry == null || inboxEntry.isDone()) {
            throw new IllegalArgumentException("No pending inbox entry found");
        }
        switch (command.getCommand()) {
            case REJECT -> rejectInboxEntry(inboxEntry, command.getRejectReason());
            case IMPORT -> importUpload(inboxEntry, command);
            case ACTIVATE_STATION -> updateStationActiveState(inboxEntry, true);
            case DEACTIVATE_STATION -> updateStationActiveState(inboxEntry, false);
            case DELETE_STATION -> deleteStation(inboxEntry);
            case DELETE_PHOTO -> deletePhoto(inboxEntry);
            case MARK_SOLVED -> markProblemReportSolved(inboxEntry);
            case CHANGE_NAME -> {
                if (StringUtils.isBlank(command.getTitle())) {
                    throw new IllegalArgumentException("Empty new title: " + command.getTitle());
                }
                changeStationTitle(inboxEntry, command.getTitle());
            }
            case UPDATE_LOCATION -> updateLocation(inboxEntry, command);
            default -> throw new IllegalArgumentException("Unexpected command value: " + command.getCommand());
        }
    }

    private void updateLocation(final InboxEntry inboxEntry, final InboxEntry command) {
        Coordinates coordinates = inboxEntry.getCoordinates();
        if (command.hasCoords()) {
            coordinates = command.getCoordinates();
        }
        if (coordinates == null || !coordinates.isValid()) {
            throw new IllegalArgumentException("Can't update location, coordinates: " + command.getCommand());
        }

        final Station station = assertStationExists(inboxEntry);
        photoStationsService.updateLocation(station, coordinates);
        inboxDao.done(inboxEntry.getId());
    }

    public int countPendingInboxEntries() {
        return inboxDao.countPendingInboxEntries();
    }

    public String getNextZ() {
        return photoStationsService.getNextZ();
    }

    private void updateStationActiveState(final InboxEntry inboxEntry, final boolean active) {
        final Station station = assertStationExists(inboxEntry);
        station.setActive(active);
        photoStationsService.updateActive(station);
        inboxDao.done(inboxEntry.getId());
        LOG.info("Problem report {} station {} set active to {}", inboxEntry.getId(), station.getKey(), active);
    }

    private void changeStationTitle(final InboxEntry inboxEntry, final String newTitle) {
        final Station station = assertStationExists(inboxEntry);
        photoStationsService.changeStationTitle(station, newTitle);
        inboxDao.done(inboxEntry.getId());
        LOG.info("Problem report {} station {} change name to {}", inboxEntry.getId(), station.getKey(), newTitle);
    }

    private void deleteStation(final InboxEntry inboxEntry) {
        final Station station = assertStationExists(inboxEntry);
        photoDao.delete(station.getKey());
        photoStationsService.delete(station);
        inboxDao.done(inboxEntry.getId());
        LOG.info("Problem report {} station {} deleted", inboxEntry.getId(), station.getKey());
    }

    private void deletePhoto(final InboxEntry inboxEntry) {
        final Station station = assertStationExists(inboxEntry);
        photoDao.delete(station.getKey());
        inboxDao.done(inboxEntry.getId());
        LOG.info("Problem report {} photo of station {} deleted", inboxEntry.getId(), station.getKey());
    }

    private void markProblemReportSolved(final InboxEntry inboxEntry) {
        assertStationExists(inboxEntry);
        inboxDao.done(inboxEntry.getId());
        LOG.info("Problem report {} accepted", inboxEntry.getId());
    }

    private Station assertStationExists(final InboxEntry inboxEntry) {
        return photoStationsService.findByCountryAndId(inboxEntry.getCountryCode(), inboxEntry.getStationId())
                .orElseThrow(() -> new IllegalArgumentException("Station not found"));
    }

    private void importUpload(final InboxEntry inboxEntry, final InboxEntry command) {
        LOG.info("Importing upload {}, {}", inboxEntry.getId(), inboxEntry.getFilename());

        Optional<Station> station = photoStationsService.findByCountryAndId(inboxEntry.getCountryCode(), inboxEntry.getStationId());
        if (station.isEmpty() && command.createStation()) {
            station = photoStationsService.findByCountryAndId(command.getCountryCode(), command.getStationId());
            station.ifPresent(value -> LOG.info("Importing missing station upload {} to existing station {}", inboxEntry.getId(), value.getKey()));
        }
        if (station.isEmpty()) {
            if (!command.createStation() || StringUtils.isNotBlank(inboxEntry.getStationId())) {
                throw new IllegalArgumentException("Station not found");
            }

            // create station
            final Optional<Country> country = countryDao.findById(StringUtils.lowerCase(command.getCountryCode()));
            if (country.isEmpty()) {
                throw new IllegalArgumentException("Country not found");
            }
            if (StringUtils.isBlank(command.getStationId())) {
                throw new IllegalArgumentException("Station ID can't be empty");
            }
            if (hasConflict(inboxEntry.getId(), inboxEntry.getCoordinates()) && !command.ignoreConflict()) {
                throw new IllegalArgumentException("There is a conflict with a nearby station");
            }
            if (command.hasCoords() && !command.getCoordinates().isValid()) {
                throw new IllegalArgumentException("Lat/Lon out of range");
            }

            Coordinates coordinates = inboxEntry.getCoordinates();
            if (command.hasCoords()) {
                coordinates = command.getCoordinates();
            }

            final String title = command.getTitle() != null ? command.getTitle() : inboxEntry.getTitle();

            station = Optional.of(new Station(new Station.Key(command.getCountryCode(), command.getStationId()), title, coordinates, command.getDs100(), null, command.getActive()));
            photoStationsService.insert(station.get());
        }

        if (station.get().hasPhoto() && !command.ignoreConflict()) {
            throw new IllegalArgumentException("Station already has a photo");
        }
        if (hasConflict(inboxEntry.getId(), station.get()) && !command.ignoreConflict()) {
            throw new IllegalArgumentException("There is a conflict with another upload");
        }

        final Optional<User> user = userDetailsService.findById(inboxEntry.getPhotographerId());
        final Country country = countryDao.findById(StringUtils.lowerCase(station.get().getKey().getCountry()))
                .orElseThrow(() -> new IllegalArgumentException("Country not found"));

        try {
            final Photo photo = new Photo(country, station.get().getKey().getId(), user.orElseThrow(), inboxEntry.getExtension());
            if (station.get().hasPhoto()) {
                photoDao.update(photo);
            } else {
                photoDao.insert(photo);
            }
            station.get().setPhoto(photo);

            photoStorage.importPhoto(inboxEntry, country, station.get());
            inboxDao.done(inboxEntry.getId());
            LOG.info("Upload {} accepted: {}", inboxEntry.getId(), inboxEntry.getFilename());
            mastodonBot.tootNewPhoto(photoStationsService.findByKey(station.get().getKey()).orElseThrow(), inboxEntry);
        } catch (final Exception e) {
            LOG.error("Error importing upload {} photo {}", inboxEntry.getId(), inboxEntry.getFilename());
            throw new RuntimeException("Error moving file: " + e.getMessage());
        }
    }

    private void rejectInboxEntry(final InboxEntry inboxEntry, final String rejectReason) {
        inboxDao.reject(inboxEntry.getId(), rejectReason);
        if (inboxEntry.isProblemReport()) {
            LOG.info("Rejecting problem report {}, {}", inboxEntry.getId(), rejectReason);
            return;
        }

        LOG.info("Rejecting upload {}, {}, {}", inboxEntry.getId(), rejectReason, inboxEntry.getFilename());

        try {
            photoStorage.reject(inboxEntry);
        } catch (final IOException e) {
            LOG.warn("Unable to move rejected file {}", inboxEntry.getFilename(), e);
        }
    }

    public InboxResponse uploadPhoto(final String clientInfo, final InputStream body, final String stationId,
                                      final String country, final String contentType, final String stationTitle,
                                      final Double latitude, final Double longitude, final String comment,
                                      final Boolean active, final User user) {
        if (!user.isEmailVerified()) {
            LOG.info("Photo upload failed for user {}, email not verified", user.getName());
            return new InboxResponse(InboxResponse.InboxResponseState.UNAUTHORIZED,"Email not verified");
        }

        final Optional<Station> station = photoStationsService.findByCountryAndId(country, stationId);
        Coordinates coordinates = null;
        if (station.isEmpty()) {
            LOG.warn("Station not found");
            if (StringUtils.isBlank(stationTitle) || latitude == null || longitude == null) {
                LOG.warn("Not enough data for missing station: title={}, latitude={}, longitude={}", stationTitle, latitude, longitude);
                return new InboxResponse(InboxResponse.InboxResponseState.NOT_ENOUGH_DATA, "Not enough data: either 'country' and 'stationId' or 'title', 'latitude' and 'longitude' have to be provided");
            }
            coordinates = new Coordinates(latitude, longitude);
            if (!coordinates.isValid()) {
                LOG.warn("Lat/Lon out of range: latitude={}, longitude={}", latitude, longitude);
                return new InboxResponse(InboxResponse.InboxResponseState.LAT_LON_OUT_OF_RANGE, "'latitude' and/or 'longitude' out of range");
            }
        }

        final String extension = ImageUtil.mimeToExtension(contentType);
        if (extension == null) {
            LOG.warn("Unknown contentType '{}'", contentType);
            return new InboxResponse(InboxResponse.InboxResponseState.UNSUPPORTED_CONTENT_TYPE, "unsupported content type (only jpg and png are supported)");
        }

        final boolean conflict = hasConflict(null, station.orElse(null)) || hasConflict(null, coordinates);

        final String filename;
        final String inboxUrl;
        final Integer id;
        final Long crc32;
        try {
            if (station.isPresent()) {
                // existing station
                id = inboxDao.insert(new InboxEntry(station.get().getKey().getCountry(), station.get().getKey().getId(), stationTitle,
                        null, user.getId(), extension, comment, null, active));
            } else {
                // missing station
                id = inboxDao.insert(new InboxEntry(country, null, stationTitle,
                        coordinates, user.getId(), extension, comment, null, active));
            }
            filename = InboxEntry.getFilename(id, extension);
            crc32 = photoStorage.storeUpload(body, filename);
            inboxDao.updateCrc32(id, crc32);

            final String duplicateInfo = conflict ? " (possible duplicate!)" : "";
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
        } catch (final PhotoStorage.PhotoTooLargeException e) {
            return new InboxResponse(InboxResponse.InboxResponseState.PHOTO_TOO_LARGE, "Photo too large, max " + e.getMaxSize() + " bytes allowed");
        } catch (final IOException e) {
            LOG.error("Error uploading photo", e);
            return new InboxResponse(InboxResponse.InboxResponseState.ERROR);
        }

        return new InboxResponse(conflict ? InboxResponse.InboxResponseState.CONFLICT : InboxResponse.InboxResponseState.REVIEW, id, filename, inboxUrl, crc32);
    }

    private boolean hasConflict(final Integer id, final Station station) {
        if (station == null) {
            return false;
        }
        if (station.hasPhoto()) {
            return true;
        }
        return inboxDao.countPendingInboxEntriesForStation(id, station.getKey().getCountry(), station.getKey().getId()) > 0;
    }

    private boolean hasConflict(final Integer id, final Coordinates coordinates) {
        if (coordinates == null || coordinates.hasZeroCoords()) {
            return false;
        }
        return inboxDao.countPendingInboxEntriesForNearbyCoordinates(id, coordinates) > 0 || photoStationsService.countNearbyCoordinates(coordinates) > 0;
    }

}
