package org.railwaystations.rsapi.core.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.railwaystations.rsapi.adapter.out.db.CountryDao;
import org.railwaystations.rsapi.adapter.out.db.InboxDao;
import org.railwaystations.rsapi.adapter.out.db.PhotoDao;
import org.railwaystations.rsapi.adapter.out.db.StationDao;
import org.railwaystations.rsapi.adapter.out.db.UserDao;
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

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboxServiceTest {

    private static final String ANY_COUNTRY = "de";

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

    @BeforeEach
    void setup() {
        inboxService = new InboxService(stationDao, photoStorage, monitor, inboxDao, userDao, countryDao, photoDao, "inboxBaseUrl", mastodonBot);
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

        @Test
        void noInboxEntryFound() {
            var command = InboxCommand.builder()
                    .id(1)
                    .build();

            assertThatThrownBy(() -> inboxService.importUpload(command)).isInstanceOf(IllegalArgumentException.class).hasMessage("No pending inbox entry found");
        }

        @Test
        void noPendingInboxEntryFound() {
            var command = InboxCommand.builder()
                    .id(1)
                    .build();

            when(inboxDao.findById(1)).thenReturn(InboxEntry.builder()
                    .done(true)
                    .build());

            assertThatThrownBy(() -> inboxService.importUpload(command)).isInstanceOf(IllegalArgumentException.class).hasMessage("No pending inbox entry found");
        }

        @Test
        void problemReportCantBeImported() {
            var command = InboxCommand.builder()
                    .id(1)
                    .build();
            when(inboxDao.findById(1)).thenReturn(InboxEntry.builder()
                            .problemReportType(ProblemReportType.OTHER)
                    .build());

            assertThatThrownBy(() -> inboxService.importUpload(command)).isInstanceOf(IllegalArgumentException.class).hasMessage("Can't import a problem report");
        }

        @Test
        void stationNotFoundAndNotCreated() {
            var command = InboxCommand.builder()
                    .id(1)
                    .createStation(false)
                    .build();
            when(inboxDao.findById(1)).thenReturn(InboxEntry.builder()
                    .countryCode("de")
                    .stationId("1")
                    .build());

            assertThatThrownBy(() -> inboxService.importUpload(command)).isInstanceOf(IllegalArgumentException.class).hasMessage("Station not found");
        }

        @Test
        void stationHasPhotoAndNoConflictResolutionProvided() {
            var command = InboxCommand.builder()
                    .id(1)
                    .conflictResolution(InboxCommand.ConflictResolution.DO_NOTHING)
                    .build();
            when(inboxDao.findById(1)).thenReturn(InboxEntry.builder()
                    .countryCode("de")
                    .stationId("1")
                    .build());
            when(stationDao.findByKey("de", "1")).thenReturn(Set.of(Station.builder()
                    .photo(Photo.builder().build()).build()));

            assertThatThrownBy(() -> inboxService.importUpload(command)).isInstanceOf(IllegalArgumentException.class).hasMessage("Station already has a photo");
        }

    }

}