package org.railwaystations.rsapi.core.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import org.railwaystations.rsapi.core.model.InboxResponse;
import org.railwaystations.rsapi.core.model.License;
import org.railwaystations.rsapi.core.model.Photo;
import org.railwaystations.rsapi.core.model.ProblemReport;
import org.railwaystations.rsapi.core.model.ProblemReportType;
import org.railwaystations.rsapi.core.model.Station;
import org.railwaystations.rsapi.core.model.User;
import org.railwaystations.rsapi.core.ports.out.MastodonBot;
import org.railwaystations.rsapi.core.ports.out.Monitor;
import org.railwaystations.rsapi.core.ports.out.PhotoStorage;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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
    static final long EXISTING_PHOTO_ID = 1L;
    static final long IMPORTED_PHOTO_ID = 2L;
    static final String IMPORTED_PHOTO_URL_PATH = "/de/1.jpg";
    static final Coordinates NEW_COORDINATES = new Coordinates(1, 2);
    static final String NEW_STATION_ID = "Z1";
    static final String NEW_STATION_TITLE = "New Station";
    static final long NEW_PHOTO_ID = 0L;

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
    final Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());

    @BeforeEach
    void setup() {
        inboxService = new InboxService(stationDao, photoStorage, monitor, inboxDao, userDao, countryDao, photoDao, "inboxBaseUrl", mastodonBot, "photoBaseUrl", clock, "stationUrl");
        reset(stationDao, photoStorage, monitor, inboxDao, userDao, countryDao, photoDao, mastodonBot);

        lenient().when(countryDao.findById(DE.getCode())).thenReturn(Optional.of(DE));
        lenient().when(userDao.findById(PHOTOGRAPHER.getId())).thenReturn(Optional.of(PHOTOGRAPHER));
    }

    @Test
    void licenseOfPhotoShouldBeTheLicenseOfUser() {
        var licenseForPhoto = InboxService.getLicenseForPhoto(createValidUser().build(), createCountryWithOverrideLicense(null));
        assertThat(licenseForPhoto).isEqualTo(License.CC0_10);
    }

    @Test
    void licenseOfPhotoShouldBeOverridenByLicenseOfCountry() {
        var licenseForPhoto = InboxService.getLicenseForPhoto(createValidUser().build(), createCountryWithOverrideLicense(License.CC_BY_NC_SA_30_DE));
        assertThat(licenseForPhoto).isEqualTo(License.CC_BY_NC_SA_30_DE);
    }

    @Test
    void postNewPhotoToMastodon() {
        var user = User.builder()
                .id(0)
                .name("name")
                .url("url")
                .license(License.CC0_10)
                .email("email")
                .anonymous(false)
                .build();
        var inboxEntry = InboxEntry.builder()
                .countryCode("de")
                .stationId("1234")
                .title("title")
                .photographerId(0)
                .photoId(2L)
                .comment("comment")
                .build();
        when(userDao.findById(0)).thenReturn(Optional.of(user));
        when(inboxDao.findRecentlyImportedPhotosNotYetPosted()).thenReturn(List.of(inboxEntry));

        inboxService.postRecentlyImportedPhotoNotYetPosted();

        verify(mastodonBot).tootNewPhoto("""
                title
                by name
                stationUrl?countryCode=de&stationId=1234&photoId=2
                comment""");
    }

    static Country createCountryWithOverrideLicense(License overrideLicense) {
        return Country.builder()
                .code("xx")
                .overrideLicense(overrideLicense)
                .build();
    }

    static User.UserBuilder createValidUser() {
        return User.builder()
                .name("name")
                .email("email@example.com")
                .license(License.CC0_10)
                .ownPhotos(true)
                .emailVerification(User.EMAIL_VERIFIED);
    }

    @Nested
    class ImportPhoto {

        @Test
        void importPhotoForExistingStation() throws IOException {
            var command = createInboxCommand1().build();
            var inboxEntry = createInboxEntry1().build();
            when(inboxDao.findById(INBOX_ENTRY1_ID)).thenReturn(inboxEntry);
            var station = createStationDe1().build();
            when(stationDao.findByKey(STATION_KEY_DE_1.getCountry(), STATION_KEY_DE_1.getId())).thenReturn(Optional.of(station));
            when(photoDao.insert(photoCaptor.capture())).thenReturn(IMPORTED_PHOTO_ID);
            when(photoStorage.importPhoto(inboxEntry, station)).thenReturn(IMPORTED_PHOTO_URL_PATH);

            inboxService.importPhoto(command);

            assertPhotoCapture(NEW_PHOTO_ID, STATION_KEY_DE_1, true);
            verify(photoStorage).importPhoto(inboxEntry, station);
            verify(inboxDao).done(inboxEntry.getId());
        }

        @Test
        void importPhotoForExistingStationWithPhotoAsNewPrimary() throws IOException {
            var command = createInboxCommand1()
                    .conflictResolution(InboxCommand.ConflictResolution.IMPORT_AS_NEW_PRIMARY_PHOTO)
                    .build();
            var inboxEntry = createInboxEntry1().build();
            when(inboxDao.findById(INBOX_ENTRY1_ID)).thenReturn(inboxEntry);
            var station = createStationDe1().build();
            whenStation1ExistsWithPhoto();
            when(photoDao.insert(photoCaptor.capture())).thenReturn(IMPORTED_PHOTO_ID);
            when(photoStorage.importPhoto(inboxEntry, station)).thenReturn(IMPORTED_PHOTO_URL_PATH);

            inboxService.importPhoto(command);

            assertPhotoCapture(NEW_PHOTO_ID, STATION_KEY_DE_1, true);
            verify(photoDao).setAllPhotosForStationSecondary(STATION_KEY_DE_1);
            verify(photoStorage).importPhoto(inboxEntry, station);
            verify(inboxDao).done(inboxEntry.getId());
        }

        @Test
        void importPhotoForExistingStationWithPhotoAsNewSecondary() throws IOException {
            var command = createInboxCommand1()
                    .conflictResolution(InboxCommand.ConflictResolution.IMPORT_AS_NEW_SECONDARY_PHOTO)
                    .build();
            var inboxEntry = createInboxEntry1().build();
            when(inboxDao.findById(INBOX_ENTRY1_ID)).thenReturn(inboxEntry);
            var station = createStationDe1().build();
            whenStation1ExistsWithPhoto();
            when(photoDao.insert(photoCaptor.capture())).thenReturn(IMPORTED_PHOTO_ID);
            when(photoStorage.importPhoto(inboxEntry, station)).thenReturn(IMPORTED_PHOTO_URL_PATH);

            inboxService.importPhoto(command);

            assertPhotoCapture(NEW_PHOTO_ID, STATION_KEY_DE_1, false);
            verify(photoDao, never()).setAllPhotosForStationSecondary(STATION_KEY_DE_1);
            verify(photoStorage).importPhoto(inboxEntry, station);
            verify(inboxDao).done(inboxEntry.getId());
        }

        @Test
        void importPhotoForExistingStationWithPhotoOverwrite() throws IOException {
            var command = createInboxCommand1()
                    .conflictResolution(InboxCommand.ConflictResolution.OVERWRITE_EXISTING_PHOTO)
                    .build();
            var inboxEntry = createInboxEntry1().build();
            when(inboxDao.findById(INBOX_ENTRY1_ID)).thenReturn(inboxEntry);
            var station = createStationDe1().build();
            whenStation1ExistsWithPhoto();
            when(photoStorage.importPhoto(inboxEntry, station)).thenReturn(IMPORTED_PHOTO_URL_PATH);

            inboxService.importPhoto(command);

            verify(photoDao).update(photoCaptor.capture());
            assertPhotoCapture(EXISTING_PHOTO_ID, STATION_KEY_DE_1, true);
            verify(photoDao, never()).setAllPhotosForStationSecondary(STATION_KEY_DE_1);
            verify(photoStorage).importPhoto(inboxEntry, station);
            verify(inboxDao).done(inboxEntry.getId());
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
        void stationNotFound() {
            var command = createInboxCommand1().build();
            when(inboxDao.findById(INBOX_ENTRY1_ID)).thenReturn(createInboxEntry1().build());

            assertThatThrownBy(() -> inboxService.importPhoto(command)).isInstanceOf(IllegalArgumentException.class).hasMessage("Station not found");
        }

        @Test
        void stationHasPhotoAndNoConflictResolutionProvided() {
            var command = createInboxCommand1().build();
            when(inboxDao.findById(INBOX_ENTRY1_ID)).thenReturn(createInboxEntry1().build());
            whenStation1ExistsWithPhoto();

            assertThatThrownBy(() -> inboxService.importPhoto(command)).isInstanceOf(IllegalArgumentException.class).hasMessage("There is a conflict with another photo");
        }

        @Test
        void stationHasNoPhotoButAnotherUploadsForThisStationExistsAndNoConflictResolutionProvided() {
            var command = createInboxCommand1().build();
            InboxEntry inboxEntry = createInboxEntry1().build();
            when(inboxDao.findById(INBOX_ENTRY1_ID)).thenReturn(inboxEntry);
            when(inboxDao.countPendingInboxEntriesForStation(INBOX_ENTRY1_ID, inboxEntry.getCountryCode(), inboxEntry.getStationId())).thenReturn(1);
            when(stationDao.findByKey(STATION_KEY_DE_1.getCountry(), STATION_KEY_DE_1.getId())).thenReturn(Optional.of(createStationDe1().build()));

            assertThatThrownBy(() -> inboxService.importPhoto(command)).isInstanceOf(IllegalArgumentException.class).hasMessage("There is a conflict with another photo");
        }

        @Test
        void stationHasNoPhotoButAnotherUploadsForThisStationExistsAndWrongConflictResolutionProvided() {
            var command = createInboxCommand1()
                    .conflictResolution(InboxCommand.ConflictResolution.OVERWRITE_EXISTING_PHOTO)
                    .build();
            InboxEntry inboxEntry = createInboxEntry1().build();
            when(inboxDao.findById(INBOX_ENTRY1_ID)).thenReturn(inboxEntry);
            when(inboxDao.countPendingInboxEntriesForStation(INBOX_ENTRY1_ID, inboxEntry.getCountryCode(), inboxEntry.getStationId())).thenReturn(1);
            when(stationDao.findByKey(STATION_KEY_DE_1.getCountry(), STATION_KEY_DE_1.getId())).thenReturn(Optional.of(createStationDe1().build()));

            assertThatThrownBy(() -> inboxService.importPhoto(command)).isInstanceOf(IllegalArgumentException.class).hasMessage("Conflict with another upload! The only possible ConflictResolution strategy is IMPORT_AS_NEW_PRIMARY_PHOTO.");
        }

    }

    private void assertPhotoCapture(long id, Station.Key stationKeyDe1, boolean primary) {
        assertThat(photoCaptor.getValue()).usingRecursiveComparison().ignoringFields("createdAt")
                .isEqualTo(Photo.builder()
                        .id(id)
                        .stationKey(stationKeyDe1)
                        .urlPath(IMPORTED_PHOTO_URL_PATH)
                        .photographer(PHOTOGRAPHER)
                        .createdAt(Instant.now())
                        .license(PHOTOGRAPHER.getLicense())
                        .primary(primary)
                        .build());
    }

    private void whenStation1ExistsWithPhoto() {
        var stationDe1 = createStationDe1().build();
        stationDe1.getPhotos().add(Photo.builder()
                .id(EXISTING_PHOTO_ID)
                .primary(true)
                .build());
        when(stationDao.findByKey(STATION_KEY_DE_1.getCountry(), STATION_KEY_DE_1.getId())).thenReturn(Optional.of(stationDe1));
    }

    private void whenStation1Exists() {
        when(stationDao.findByKey(STATION_KEY_DE_1.getCountry(), STATION_KEY_DE_1.getId())).thenReturn(Optional.of(createStationDe1().build()));
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
                .stationId(NEW_STATION_ID)
                .title(NEW_STATION_TITLE)
                .coordinates(NEW_COORDINATES)
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

    @Nested
    class ImportMissingStation {

        @Test
        void importPhotoForNewStation() throws IOException {
            var command = createNewStationCommand1().build();
            var inboxEntry = createInboxEntry1()
                    .stationId(null)
                    .build();
            when(inboxDao.findById(INBOX_ENTRY1_ID)).thenReturn(inboxEntry);
            when(stationDao.findByKey(STATION_KEY_DE_1.getCountry(), command.getStationId())).thenReturn(Optional.empty());
            var newStation = createNewStationByCommand(command).build();
            when(photoDao.insert(photoCaptor.capture())).thenReturn(IMPORTED_PHOTO_ID);
            when(photoStorage.importPhoto(inboxEntry, newStation)).thenReturn(IMPORTED_PHOTO_URL_PATH);

            inboxService.importMissingStation(command);

            verify(inboxDao).countPendingInboxEntriesForNearbyCoordinates(command.getId(), command.getCoordinates());
            verify(stationDao).countNearbyCoordinates(command.getCoordinates());
            verify(stationDao).insert(newStation);
            assertPhotoCapture(NEW_PHOTO_ID, newStation.getKey(), true);
            verify(photoStorage).importPhoto(inboxEntry, newStation);
            verify(inboxDao).updateMissingStationImported(inboxEntry.getId(), STATION_KEY_DE_1.getCountry(), NEW_STATION_ID, NEW_STATION_TITLE);
        }

        @Test
        void createNewStationWithoutPhoto() throws IOException {
            var command = createNewStationCommand1().build();
            var inboxEntry = createInboxEntry1()
                    .stationId(null)
                    .extension(null)
                    .build();
            when(inboxDao.findById(INBOX_ENTRY1_ID)).thenReturn(inboxEntry);
            when(stationDao.findByKey(STATION_KEY_DE_1.getCountry(), command.getStationId())).thenReturn(Optional.empty());
            var newStation = createNewStationByCommand(command).build();

            inboxService.importMissingStation(command);

            verify(inboxDao).countPendingInboxEntriesForNearbyCoordinates(command.getId(), command.getCoordinates());
            verify(stationDao).countNearbyCoordinates(command.getCoordinates());
            verify(stationDao).insert(newStation);
            verify(photoDao, never()).insert(any(Photo.class));
            verify(photoStorage, never()).importPhoto(any(InboxEntry.class), any(Station.class));
            verify(inboxDao).updateMissingStationImported(inboxEntry.getId(), STATION_KEY_DE_1.getCountry(), NEW_STATION_ID, NEW_STATION_TITLE);
        }

        @Test
        void noInboxEntryFound() {
            var command = createInboxCommand1().build();

            assertThatThrownBy(() -> inboxService.importMissingStation(command)).isInstanceOf(IllegalArgumentException.class).hasMessage("No pending inbox entry found");
        }

        @Test
        void noPendingInboxEntryFound() {
            var command = createInboxCommand1().build();
            when(inboxDao.findById(INBOX_ENTRY1_ID)).thenReturn(createInboxEntry1()
                    .done(true)
                    .build());

            assertThatThrownBy(() -> inboxService.importMissingStation(command)).isInstanceOf(IllegalArgumentException.class).hasMessage("No pending inbox entry found");
        }

        @Test
        void problemReportCantBeImported() {
            var command = createInboxCommand1().build();
            when(inboxDao.findById(INBOX_ENTRY1_ID)).thenReturn(createInboxEntry1()
                    .problemReportType(ProblemReportType.OTHER)
                    .build());

            assertThatThrownBy(() -> inboxService.importMissingStation(command)).isInstanceOf(IllegalArgumentException.class).hasMessage("Can't import a problem report");
        }

        @Test
        void stationNotFoundAndNotCreatedBecauseCountryNotFound() {
            var command = createNewStationCommand1()
                    .countryCode("xx")
                    .build();
            when(inboxDao.findById(INBOX_ENTRY1_ID)).thenReturn(createInboxEntry1().build());

            assertThatThrownBy(() -> inboxService.importMissingStation(command)).isInstanceOf(IllegalArgumentException.class).hasMessage("Country not found");
        }

        @Test
        void stationNotFoundAndNotCreatedBecauseNoValidCoordinatesProvides() {
            var command = createNewStationCommand1()
                    .coordinates(new Coordinates(500, -300))
                    .build();
            when(inboxDao.findById(INBOX_ENTRY1_ID)).thenReturn(createInboxEntry1().build());

            assertThatThrownBy(() -> inboxService.importMissingStation(command)).isInstanceOf(IllegalArgumentException.class).hasMessage("No valid coordinates provided");
        }

        @Test
        void stationNotFoundAndNotCreatedBecauseNoCoordinatesProvides() {
            var command = createNewStationCommand1()
                    .coordinates(null)
                    .build();
            when(inboxDao.findById(INBOX_ENTRY1_ID)).thenReturn(createInboxEntry1().build());

            assertThatThrownBy(() -> inboxService.importMissingStation(command)).isInstanceOf(IllegalArgumentException.class).hasMessage("No valid coordinates provided");
        }

        @Test
        void stationNotFoundAndNotCreatedBecauseTitleIsEmpty() {
            var command = createNewStationCommand1()
                    .title(null)
                    .build();
            when(inboxDao.findById(INBOX_ENTRY1_ID)).thenReturn(createInboxEntry1().build());

            assertThatThrownBy(() -> inboxService.importMissingStation(command)).isInstanceOf(IllegalArgumentException.class).hasMessage("Station title can't be empty");
        }

        @Test
        void stationNotFoundAndNotCreatedBecauseNoActiveFlagProvided() {
            var command = createNewStationCommand1()
                    .active(null)
                    .build();
            when(inboxDao.findById(INBOX_ENTRY1_ID)).thenReturn(createInboxEntry1().build());

            assertThatThrownBy(() -> inboxService.importMissingStation(command)).isInstanceOf(IllegalArgumentException.class).hasMessage("No Active flag provided");
        }

        @Test
        void stationHasPhotoAndNoConflictResolutionProvided() {
            var command = createInboxCommand1()
                    .countryCode(STATION_KEY_DE_1.getCountry())
                    .stationId(STATION_KEY_DE_1.getId())
                    .build();
            when(inboxDao.findById(INBOX_ENTRY1_ID)).thenReturn(createInboxEntry1().build());
            whenStation1ExistsWithPhoto();

            assertThatThrownBy(() -> inboxService.importMissingStation(command)).isInstanceOf(IllegalArgumentException.class).hasMessage("There is a conflict with another photo");
        }

        @Test
        void stationHasNoPhotoButAnotherUploadsForThisStationExistsAndNoConflictResolutionProvided() {
            var command = createInboxCommand1()
                    .countryCode(STATION_KEY_DE_1.getCountry())
                    .stationId(STATION_KEY_DE_1.getId())
                    .build();
            InboxEntry inboxEntry = createInboxEntry1().build();
            when(inboxDao.findById(INBOX_ENTRY1_ID)).thenReturn(inboxEntry);
            when(inboxDao.countPendingInboxEntriesForStation(INBOX_ENTRY1_ID, inboxEntry.getCountryCode(), inboxEntry.getStationId())).thenReturn(1);
            when(stationDao.findByKey(STATION_KEY_DE_1.getCountry(), STATION_KEY_DE_1.getId())).thenReturn(Optional.of(createStationDe1().build()));

            assertThatThrownBy(() -> inboxService.importMissingStation(command)).isInstanceOf(IllegalArgumentException.class).hasMessage("There is a conflict with another photo");
        }

    }

    @Nested
    class ReportProblem {
        @Test
        void reportWrongPhoto() {
            var problemReport = createWrongPhotoProblemReport(EXISTING_PHOTO_ID);
            whenStation1ExistsWithPhoto();

            var inboxResponse = inboxService.reportProblem(problemReport, createValidUser().build(), null);

            assertThat(inboxResponse.getState()).isEqualTo(InboxResponse.InboxResponseState.REVIEW);
        }

        @Test
        void reportWrongPhotoWithWrongPhotoId() {
            var problemReport = createWrongPhotoProblemReport(0L);
            whenStation1ExistsWithPhoto();

            var inboxResponse = inboxService.reportProblem(problemReport, createValidUser().build(), null);

            assertThat(inboxResponse.getState()).isEqualTo(InboxResponse.InboxResponseState.NOT_ENOUGH_DATA);
            assertThat(inboxResponse.getMessage()).isEqualTo("Photo with this id not found at station");
        }

        @ParameterizedTest
        @MethodSource("invalidUsersForReportProblem")
        void reportProblemWithInvalidUser(User invalidUser) {
            var problemReport = createWrongPhotoProblemReport(EXISTING_PHOTO_ID);

            var inboxResponse = inboxService.reportProblem(problemReport, invalidUser, null);

            assertThat(inboxResponse.getState()).isEqualTo(InboxResponse.InboxResponseState.UNAUTHORIZED);
            assertThat(inboxResponse.getMessage()).isEqualTo("Profile incomplete");
        }

        static List<Arguments> invalidUsersForReportProblem() {
            return List.of(
                    Arguments.of(createValidUser().name(null).build()),
                    Arguments.of(createValidUser().email(null).build()),
                    Arguments.of(createValidUser().emailVerification("SOME_TOKEN").build())
            );
        }

        @Test
        void reportWrongPhotoForStationWithoutPhoto() {
            var problemReport = createWrongPhotoProblemReport(EXISTING_PHOTO_ID);
            whenStation1Exists();

            var inboxResponse = inboxService.reportProblem(problemReport, createValidUser().build(), null);

            assertThat(inboxResponse.getState()).isEqualTo(InboxResponse.InboxResponseState.NOT_ENOUGH_DATA);
            assertThat(inboxResponse.getMessage()).isEqualTo("Problem type is only applicable to station with photo");
        }

        ProblemReport createWrongPhotoProblemReport(long existingPhotoId) {
            return ProblemReport.builder()
                    .countryCode(STATION_KEY_DE_1.getCountry())
                    .stationId(STATION_KEY_DE_1.getId())
                    .photoId(existingPhotoId)
                    .type(ProblemReportType.WRONG_PHOTO)
                    .comment("a comment")
                    .build();
        }

        @Test
        void changeNameCommand() {
            whenStation1Exists();
            whenWrongNameProblemReportExists();

            inboxService.changeStationTitle(InboxCommand.builder()
                    .id(INBOX_ENTRY1_ID)
                    .title("New Title By Admin")
                    .build());

            verify(stationDao).changeStationTitle(STATION_KEY_DE_1, "New Title By Admin");
            verify(inboxDao).done(INBOX_ENTRY1_ID);
        }

        @Test
        void changeNameCommandWithoutTitle() {
            whenWrongNameProblemReportExists();

            assertThatThrownBy(() -> inboxService.changeStationTitle(InboxCommand.builder()
                    .id(INBOX_ENTRY1_ID)
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Empty new title: null");
        }

        private void whenWrongNameProblemReportExists() {
            when(inboxDao.findById(INBOX_ENTRY1_ID)).thenReturn(InboxEntry.builder()
                    .id(INBOX_ENTRY1_ID)
                    .problemReportType(ProblemReportType.WRONG_NAME)
                    .countryCode(STATION_KEY_DE_1.getCountry())
                    .stationId(STATION_KEY_DE_1.getId())
                    .title("Old Title")
                    .newTitle("New Title By Reporter")
                    .build());
        }

        @Test
        void changeLocationCommandWithCoordinatesByAdmin() {
            whenStation1Exists();
            whenWrongLocationInboxEntryExists();

            inboxService.updateLocation(InboxCommand.builder()
                    .id(INBOX_ENTRY1_ID)
                    .coordinates(new Coordinates(52.0, 11.0))
                    .build());

            verify(stationDao).updateLocation(STATION_KEY_DE_1, new Coordinates(52.0, 11.0));
            verify(inboxDao).done(INBOX_ENTRY1_ID);
        }

        private void whenWrongLocationInboxEntryExists() {
            when(inboxDao.findById(INBOX_ENTRY1_ID)).thenReturn(InboxEntry.builder()
                    .id(INBOX_ENTRY1_ID)
                    .problemReportType(ProblemReportType.WRONG_LOCATION)
                    .countryCode(STATION_KEY_DE_1.getCountry())
                    .stationId(STATION_KEY_DE_1.getId())
                    .coordinates(new Coordinates(50.0, 9.0))
                    .newCoordinates(new Coordinates(51.0, 10.0))
                    .build());
        }

        @Test
        void changeLocationCommandWithoutNewCoordinatesByAdmin() {
            whenWrongLocationInboxEntryExists();

            assertThatThrownBy(() -> inboxService.updateLocation(InboxCommand.builder()
                    .id(INBOX_ENTRY1_ID)
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Can't update location, coordinates: null");
        }

        @Test
        void changeLocationCommandWithZeroCoordinatesByAdmin() {
            whenWrongLocationInboxEntryExists();

            assertThatThrownBy(() -> inboxService.updateLocation(InboxCommand.builder()
                    .id(INBOX_ENTRY1_ID)
                    .coordinates(new Coordinates(0, 0))
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Can't update location, coordinates: Coordinates(lat=0.0, lon=0.0)");
        }

        @Test
        void changeLocationCommandWithInvalidCoordinatesByAdmin() {
            whenWrongLocationInboxEntryExists();

            assertThatThrownBy(() -> inboxService.updateLocation(InboxCommand.builder()
                    .id(INBOX_ENTRY1_ID)
                    .coordinates(new Coordinates(110, 90))
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Can't update location, coordinates: Coordinates(lat=110.0, lon=90.0)");
        }

    }

}