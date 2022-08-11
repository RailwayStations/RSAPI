package org.railwaystations.rsapi.core.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.railwaystations.rsapi.adapter.out.db.CountryDao;
import org.railwaystations.rsapi.adapter.out.db.InboxDao;
import org.railwaystations.rsapi.adapter.out.db.PhotoDao;
import org.railwaystations.rsapi.adapter.out.db.StationDao;
import org.railwaystations.rsapi.adapter.out.db.UserDao;
import org.railwaystations.rsapi.core.model.Coordinates;
import org.railwaystations.rsapi.core.model.Country;
import org.railwaystations.rsapi.core.model.InboxCommand;
import org.railwaystations.rsapi.core.model.InboxEntry;
import org.railwaystations.rsapi.core.model.License;
import org.railwaystations.rsapi.core.model.Photo;
import org.railwaystations.rsapi.core.model.ProblemReportType;
import org.railwaystations.rsapi.core.model.Station;
import org.railwaystations.rsapi.core.model.User;
import org.railwaystations.rsapi.core.ports.out.MastodonBot;
import org.railwaystations.rsapi.core.ports.out.Monitor;
import org.railwaystations.rsapi.core.ports.out.PhotoStorage;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboxServiceTest {

    static final Country DE = Country.builder().code("de").name("Germany").active(true).build();
    static final Station.Key STATION_KEY_DE_1 = new Station.Key(DE.getCode(), "1");
    static final long INBOX_ENTRY1_ID = 1;
    static final User PHOTOGRAPHER = User.builder()
            .id(1)
            .name("nickname")
            .license(License.CC0_10)
            .build();
    InboxService inboxService;

    @Mock
    StationDao stationDao;
    @Mock
    PhotoStorage photoStorage;
    @Mock
    Monitor monitor;
    @Mock
    InboxDao inboxDao;
    @Mock
    UserDao userDao;
    @Mock
    CountryDao countryDao;
    @Mock
    PhotoDao photoDao;
    @Mock
    MastodonBot mastodonBot;
    @Captor
    ArgumentCaptor<Photo> photoCaptor;

    @BeforeEach
    void setup() {
        inboxService = new InboxService(stationDao, photoStorage, monitor, inboxDao, userDao, countryDao, photoDao, "inboxBaseUrl", mastodonBot);
        reset(stationDao, photoStorage, monitor, inboxDao, userDao, countryDao, photoDao, mastodonBot);
    }

    @Test
    void licenseOfPhotoShouldBeTheLicenseOfUser() {
        var licenseForPhoto = InboxService.getLicenseForPhoto(createUserWithCC0License(), createCountryWithOverrideLicense(null));
        assertThat(licenseForPhoto).isEqualTo(License.CC0_10);
    }

    @Test
    void licenseOfPhotoShouldBeOverridenByLicenseOfCountry() {
        var licenseForPhoto = InboxService.getLicenseForPhoto(createUserWithCC0License(), createCountryWithOverrideLicense(License.CC_BY_NC_SA_30_DE));
        assertThat(licenseForPhoto).isEqualTo(License.CC_BY_NC_SA_30_DE);
    }

    private Country createCountryWithOverrideLicense(License overrideLicense) {
        return Country.builder()
                .code("xx")
                .overrideLicense(overrideLicense)
                .build();
    }

    private User createUserWithCC0License() {
        return User.builder()
                .license(License.CC0_10)
                .build();
    }

    @Nested
    class ImportUpload {

        static final long IMPORTED_PHOTO_ID = 1L;
        static final String IMPORTED_PHOTO_URL_PATH = "/de/1.jpg";

        @Test
        void importPhotoForExistingStation() throws IOException {
            var command = createInboxCommand1().build();
            var inboxEntry = createInboxEntry1().build();
            when(inboxDao.findById(INBOX_ENTRY1_ID)).thenReturn(inboxEntry);
            var station = createStationDe1().build();
            when(stationDao.findByKey(STATION_KEY_DE_1.getCountry(), STATION_KEY_DE_1.getId())).thenReturn(Set.of(station));
            when(userDao.findById(PHOTOGRAPHER.getId())).thenReturn(Optional.of(PHOTOGRAPHER));
            when(countryDao.findById(DE.getCode())).thenReturn(Optional.of(DE));
            when(photoDao.insert(photoCaptor.capture())).thenReturn(IMPORTED_PHOTO_ID);
            when(photoStorage.importPhoto(inboxEntry, station)).thenReturn(IMPORTED_PHOTO_URL_PATH);

            inboxService.importPhoto(command);

            assertThat(photoCaptor.getValue()).usingRecursiveComparison().ignoringFields("createdAt")
                    .isEqualTo(Photo.builder()
                    .stationKey(STATION_KEY_DE_1)
                    .urlPath(IMPORTED_PHOTO_URL_PATH)
                    .photographer(PHOTOGRAPHER)
                    .createdAt(Instant.now())
                    .license(PHOTOGRAPHER.getLicense())
                    .primary(true)
                    .build());
            verify(photoStorage).importPhoto(inboxEntry, station);
            verify(inboxDao).done(inboxEntry.getId());
            verify(mastodonBot).tootNewPhoto(station, inboxEntry, photoCaptor.getValue(), IMPORTED_PHOTO_ID);
        }

        @Test
        void importPhotoForNewStation() throws IOException {
            var command = createNewStationCommand1().build();
            var inboxEntry = createInboxEntry1()
                    .stationId(null)
                    .build();
            when(inboxDao.findById(INBOX_ENTRY1_ID)).thenReturn(inboxEntry);
            when(stationDao.findByKey(STATION_KEY_DE_1.getCountry(), command.getStationId())).thenReturn(Collections.emptySet());
            var newStation = createNewStationByCommand(command).build();
            when(userDao.findById(PHOTOGRAPHER.getId())).thenReturn(Optional.of(PHOTOGRAPHER));
            when(countryDao.findById(DE.getCode())).thenReturn(Optional.of(DE));
            when(photoDao.insert(photoCaptor.capture())).thenReturn(IMPORTED_PHOTO_ID);
            when(photoStorage.importPhoto(inboxEntry, newStation)).thenReturn(IMPORTED_PHOTO_URL_PATH);

            inboxService.importMissingStation(command);

            verify(inboxDao).countPendingInboxEntriesForNearbyCoordinates(command.getId(), command.getCoordinates());
            verify(stationDao).countNearbyCoordinates(command.getCoordinates());
            verify(stationDao).insert(newStation);
            assertThat(photoCaptor.getValue()).usingRecursiveComparison().ignoringFields("createdAt")
                    .isEqualTo(Photo.builder()
                            .stationKey(newStation.getKey())
                            .urlPath(IMPORTED_PHOTO_URL_PATH)
                            .photographer(PHOTOGRAPHER)
                            .createdAt(Instant.now())
                            .license(PHOTOGRAPHER.getLicense())
                            .primary(true)
                            .build());
            verify(photoStorage).importPhoto(inboxEntry, newStation);
            verify(inboxDao).done(inboxEntry.getId());
            verify(mastodonBot).tootNewPhoto(newStation, inboxEntry, photoCaptor.getValue(), IMPORTED_PHOTO_ID);
        }

        @Test
        void createNewStationWithoutPhoto() throws IOException {
            var command = createNewStationCommand1().build();
            var inboxEntry = createInboxEntry1()
                    .stationId(null)
                    .extension(null)
                    .build();
            when(inboxDao.findById(INBOX_ENTRY1_ID)).thenReturn(inboxEntry);
            when(stationDao.findByKey(STATION_KEY_DE_1.getCountry(), command.getStationId())).thenReturn(Collections.emptySet());
            var newStation = createNewStationByCommand(command).build();
            when(countryDao.findById(DE.getCode())).thenReturn(Optional.of(DE));

            inboxService.importMissingStation(command);

            verify(inboxDao).countPendingInboxEntriesForNearbyCoordinates(command.getId(), command.getCoordinates());
            verify(stationDao).countNearbyCoordinates(command.getCoordinates());
            verify(stationDao).insert(newStation);
            verify(photoDao, never()).insert(any(Photo.class));
            verify(photoStorage, never()).importPhoto(any(InboxEntry.class), any(Station.class));
            verify(inboxDao).done(inboxEntry.getId());
            verify(mastodonBot, never()).tootNewPhoto(any(Station.class), any(InboxEntry.class), any(Photo.class), anyLong());
        }

        @Test
        void noInboxEntryFound() {
            var command = createInboxCommand1().build();

            assertThatThrownBy(() -> inboxService.importPhoto(command)).isInstanceOf(IllegalArgumentException.class).hasMessage("No pending inbox entry found");
        }

        @Test
        void noPendingInboxEntryFound() {
            var command = createInboxCommand1().build();
            when(inboxDao.findById(INBOX_ENTRY1_ID)).thenReturn(createInboxEntry1()
                    .done(true)
                    .build());

            assertThatThrownBy(() -> inboxService.importPhoto(command)).isInstanceOf(IllegalArgumentException.class).hasMessage("No pending inbox entry found");
        }

        @Test
        void problemReportCantBeImported() {
            var command = createInboxCommand1().build();
            when(inboxDao.findById(INBOX_ENTRY1_ID)).thenReturn(createInboxEntry1()
                    .problemReportType(ProblemReportType.OTHER)
                    .build());

            assertThatThrownBy(() -> inboxService.importPhoto(command)).isInstanceOf(IllegalArgumentException.class).hasMessage("No photo to import");
        }

        @Test
        void stationNotFoundAndNotCreated() {
            var command = createInboxCommand1().build();
            when(inboxDao.findById(INBOX_ENTRY1_ID)).thenReturn(createInboxEntry1().build());

            assertThatThrownBy(() -> inboxService.importPhoto(command)).isInstanceOf(IllegalArgumentException.class).hasMessage("Station not found");
        }

        @Test
        void stationHasPhotoAndNoConflictResolutionProvided() {
            var command = createInboxCommand1().build();
            when(inboxDao.findById(INBOX_ENTRY1_ID)).thenReturn(createInboxEntry1().build());
            when(stationDao.findByKey(STATION_KEY_DE_1.getCountry(), STATION_KEY_DE_1.getId())).thenReturn(Set.of(createStationDe1()
                    .photo(Photo.builder().build())
                    .build()));

            assertThatThrownBy(() -> inboxService.importPhoto(command)).isInstanceOf(IllegalArgumentException.class).hasMessage("There is a conflict with another photo");
        }

        @Test
        void stationHasNoPhotoButAnotherUploadsForThisStationExistsAndNoConflictResolutionProvided() {
            var command = createInboxCommand1().build();
            InboxEntry inboxEntry = createInboxEntry1().build();
            when(inboxDao.findById(INBOX_ENTRY1_ID)).thenReturn(inboxEntry);
            when(inboxDao.countPendingInboxEntriesForStation(INBOX_ENTRY1_ID, inboxEntry.getCountryCode(), inboxEntry.getStationId())).thenReturn(1);
            when(stationDao.findByKey(STATION_KEY_DE_1.getCountry(), STATION_KEY_DE_1.getId())).thenReturn(Set.of(createStationDe1().build()));

            assertThatThrownBy(() -> inboxService.importPhoto(command)).isInstanceOf(IllegalArgumentException.class).hasMessage("There is a conflict with another photo");
        }

    }

    private Station.StationBuilder createNewStationByCommand(InboxCommand command) {
        return Station.builder()
                .key(new Station.Key(command.getCountryCode(), command.getStationId()))
                .title(command.getTitle())
                .coordinates(command.getCoordinates())
                .ds100(command.getDs100())
                .active(command.getActive());
    }

    private InboxCommand.InboxCommandBuilder createNewStationCommand1() {
        return createInboxCommand1()
                .countryCode(DE.getCode())
                .stationId("Z1")
                .title("New Station")
                .coordinates(new Coordinates(1, 2))
                .active(true);
    }

    private Station.StationBuilder createStationDe1() {
        return Station.builder()
                .key(STATION_KEY_DE_1)
                .title("Station DE 1");
    }

    private InboxCommand.InboxCommandBuilder createInboxCommand1() {
        return InboxCommand.builder()
                .id(INBOX_ENTRY1_ID)
                .conflictResolution(InboxCommand.ConflictResolution.DO_NOTHING);
    }

    private InboxEntry.InboxEntryBuilder createInboxEntry1() {
        return InboxEntry.builder()
                .id(INBOX_ENTRY1_ID)
                .countryCode(STATION_KEY_DE_1.getCountry())
                .stationId(STATION_KEY_DE_1.getId())
                .photographerId(PHOTOGRAPHER.getId())
                .extension("jpg")
                .done(false);
    }

}