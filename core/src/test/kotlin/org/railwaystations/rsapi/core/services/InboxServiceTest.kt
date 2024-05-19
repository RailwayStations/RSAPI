package org.railwaystations.rsapi.core.services

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.railwaystations.rsapi.core.model.Coordinates
import org.railwaystations.rsapi.core.model.Country
import org.railwaystations.rsapi.core.model.InboxCommand
import org.railwaystations.rsapi.core.model.InboxEntry
import org.railwaystations.rsapi.core.model.InboxResponse.InboxResponseState
import org.railwaystations.rsapi.core.model.License
import org.railwaystations.rsapi.core.model.Photo
import org.railwaystations.rsapi.core.model.ProblemReport
import org.railwaystations.rsapi.core.model.ProblemReportType
import org.railwaystations.rsapi.core.model.Station
import org.railwaystations.rsapi.core.model.User
import org.railwaystations.rsapi.core.ports.inbound.ManageInboxUseCase
import org.railwaystations.rsapi.core.ports.outbound.CountryPort
import org.railwaystations.rsapi.core.ports.outbound.InboxPort
import org.railwaystations.rsapi.core.ports.outbound.MastodonPort
import org.railwaystations.rsapi.core.ports.outbound.MonitorPort
import org.railwaystations.rsapi.core.ports.outbound.PhotoPort
import org.railwaystations.rsapi.core.ports.outbound.PhotoStoragePort
import org.railwaystations.rsapi.core.ports.outbound.StationPort
import org.railwaystations.rsapi.core.ports.outbound.UserPort
import org.railwaystations.rsapi.core.services.InboxService.Companion.getLicenseForPhoto
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.*

internal class InboxServiceTest {
    private lateinit var inboxService: InboxService

    var stationPort = mockk<StationPort>()

    var photoStoragePort = mockk<PhotoStoragePort>()

    private var monitorPort = mockk<MonitorPort>()

    var inboxPort = mockk<InboxPort>()

    private var userPort = mockk<UserPort>()

    private var countryPort = mockk<CountryPort>()

    var photoPort = mockk<PhotoPort>()

    private var mastodonPort = mockk<MastodonPort>(relaxed = true)

    var photoCaptor = slot<Photo>()
    private val clock: Clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())

    @BeforeEach
    fun setup() {
        inboxService = InboxService(
            stationPort = stationPort,
            photoStoragePort = photoStoragePort,
            monitorPort = monitorPort,
            inboxPort = inboxPort,
            userPort = userPort,
            countryPort = countryPort,
            photoPort = photoPort,
            inboxBaseUrl = "inboxBaseUrl",
            mastodonPort = mastodonPort,
            photoBaseUrl = "photoBaseUrl",
            clock = clock,
            stationUrl = "stationUrl"
        )
        clearMocks(
            stationPort,
            photoStoragePort,
            monitorPort,
            inboxPort,
            userPort,
            countryPort,
            photoPort,
            mastodonPort
        )

        every { countryPort.findById(any()) } returns null
        every { countryPort.findById(DE.code) } returns DE
        every { inboxPort.findById(any()) } returns null
        every { inboxPort.done(any()) } returns Unit
        every { inboxPort.updatePosted(any()) } returns Unit
        every { inboxPort.updatePhotoId(any(), any()) } returns Unit
        every { inboxPort.countPendingInboxEntriesForStation(any(), any(), any()) } returns 0
        every { inboxPort.countPendingInboxEntriesForNearbyCoordinates(any(), any()) } returns 0
        every { inboxPort.updateMissingStationImported(any(), any(), any(), any()) } returns Unit
        every { inboxPort.insert(any()) } returns 0
        every { monitorPort.sendMessage(any()) } returns Unit
        every { photoPort.setAllPhotosForStationSecondary(any()) } returns Unit
        every { photoPort.update(any()) } returns Unit
        every { stationPort.findByKey(any(), any()) } returns null
        every { stationPort.countNearbyCoordinates(any()) } returns 0
        every { stationPort.insert(any()) } returns Unit
        every { stationPort.updateLocation(any(), any()) } returns Unit
        every { stationPort.changeStationTitle(any(), any()) } returns Unit
        every { userPort.findById(PHOTOGRAPHER.id) } returns PHOTOGRAPHER
    }

    @Test
    fun licenseOfPhotoShouldBeTheLicenseOfUser() {
        val licenseForPhoto = getLicenseForPhoto(createValidUser(), createCountryWithOverrideLicense(null))
        assertThat(licenseForPhoto).isEqualTo(License.CC0_10)
    }

    @Test
    fun licenseOfPhotoShouldBeOverridenByLicenseOfCountry() {
        val licenseForPhoto =
            getLicenseForPhoto(createValidUser(), createCountryWithOverrideLicense(License.CC_BY_NC_SA_30_DE))
        assertThat(licenseForPhoto).isEqualTo(License.CC_BY_NC_SA_30_DE)
    }

    @Test
    fun postNewPhotoToMastodon() {
        val user = User(
            id = 0,
            name = "name",
            url = "url",
            license = License.CC0_10,
            email = "email",
            ownPhotos = true,
            sendNotifications = true,
        )
        val inboxEntry = InboxEntry(
            countryCode = "de",
            stationId = "1234",
            photoId = 2L,
            title = "title",
            comment = "comment",
        )
        every { userPort.findById(0) } returns user
        every { inboxPort.findRecentlyImportedPhotosNotYetPosted() } returns listOf(inboxEntry)

        inboxService.postRecentlyImportedPhotoNotYetPosted()

        verify {
            mastodonPort.tootNewPhoto(
                """
                title
                by name
                stationUrl?countryCode=de&stationId=1234&photoId=2
                comment
                """.trimIndent()
            )
        }
    }

    @Nested
    internal inner class ImportPhoto {
        @Test
        fun importPhotoForExistingStation() {
            val command = createInboxCommand1()
            val inboxEntry = createInboxEntry1()
            every { inboxPort.findById(INBOX_ENTRY1_ID) } returns inboxEntry
            val station = createStationDe1()
            every { stationPort.findByKey(STATION_KEY_DE_1.country, STATION_KEY_DE_1.id) } returns station
            every { photoPort.insert(capture(photoCaptor)) } returns IMPORTED_PHOTO_ID
            every { photoStoragePort.importPhoto(inboxEntry, station) } returns IMPORTED_PHOTO_URL_PATH

            inboxService.importPhoto(command)

            assertPhotoCapture(NEW_PHOTO_ID, STATION_KEY_DE_1, true)
            verify { photoStoragePort.importPhoto(inboxEntry, station) }
            verify { inboxPort.done(inboxEntry.id) }
        }

        @Test
        @Throws(IOException::class)
        fun importPhotoForExistingStationWithPhotoAsNewPrimary() {
            val command = createInboxCommand1().copy(
                conflictResolution = InboxCommand.ConflictResolution.IMPORT_AS_NEW_PRIMARY_PHOTO
            )
            val inboxEntry = createInboxEntry1()
            every { inboxPort.findById(INBOX_ENTRY1_ID) } returns inboxEntry
            val station = createStationDe1()
            whenStation1ExistsWithPhoto()
            every { photoPort.insert(capture(photoCaptor)) } returns IMPORTED_PHOTO_ID
            every { photoStoragePort.importPhoto(inboxEntry, station) } returns IMPORTED_PHOTO_URL_PATH

            inboxService.importPhoto(command)

            assertPhotoCapture(NEW_PHOTO_ID, STATION_KEY_DE_1, true)
            verify { photoPort.setAllPhotosForStationSecondary(STATION_KEY_DE_1) }
            verify { photoStoragePort.importPhoto(inboxEntry, station) }
            verify { inboxPort.done(inboxEntry.id) }
        }

        @Test
        @Throws(IOException::class)
        fun importPhotoForExistingStationWithPhotoAsNewSecondary() {
            val command = createInboxCommand1().copy(
                conflictResolution = InboxCommand.ConflictResolution.IMPORT_AS_NEW_SECONDARY_PHOTO
            )
            val inboxEntry = createInboxEntry1()
            every { inboxPort.findById(INBOX_ENTRY1_ID) } returns inboxEntry
            val station = createStationDe1()
            whenStation1ExistsWithPhoto()
            every { photoPort.insert(capture(photoCaptor)) } returns IMPORTED_PHOTO_ID
            every { photoStoragePort.importPhoto(inboxEntry, station) } returns IMPORTED_PHOTO_URL_PATH

            inboxService.importPhoto(command)

            assertPhotoCapture(NEW_PHOTO_ID, STATION_KEY_DE_1, false)
            verify(exactly = 0) { photoPort.setAllPhotosForStationSecondary(STATION_KEY_DE_1) }
            verify { photoStoragePort.importPhoto(inboxEntry, station) }
            verify { inboxPort.done(inboxEntry.id) }
        }

        @Test
        @Throws(IOException::class)
        fun importPhotoForExistingStationWithPhotoOverwrite() {
            val command = createInboxCommand1().copy(
                conflictResolution = InboxCommand.ConflictResolution.OVERWRITE_EXISTING_PHOTO
            )
            val inboxEntry = createInboxEntry1()
            every { inboxPort.findById(INBOX_ENTRY1_ID) } returns inboxEntry
            val station = createStationDe1()
            whenStation1ExistsWithPhoto()
            every { photoStoragePort.importPhoto(inboxEntry, station) } returns IMPORTED_PHOTO_URL_PATH

            inboxService.importPhoto(command)

            verify { photoPort.update(capture(photoCaptor)) }
            assertPhotoCapture(EXISTING_PHOTO_ID, STATION_KEY_DE_1, true)
            verify(exactly = 0) { photoPort.setAllPhotosForStationSecondary(STATION_KEY_DE_1) }
            verify { photoStoragePort.importPhoto(inboxEntry, station) }
            verify { inboxPort.done(inboxEntry.id) }
        }

        @Test
        fun noInboxEntryFound() {
            val command = createInboxCommand1()

            assertThatThrownBy { inboxService.importPhoto(command) }.isInstanceOf(
                IllegalArgumentException::class.java
            ).hasMessage("No pending inbox entry found")
        }

        @Test
        fun noPendingInboxEntryFound() {
            val command = createInboxCommand1()
            val inboxEntry1 = createInboxEntry1().copy(
                done = true
            )
            every { inboxPort.findById(INBOX_ENTRY1_ID) } returns inboxEntry1

            assertThatThrownBy { inboxService.importPhoto(command) }.isInstanceOf(
                IllegalArgumentException::class.java
            ).hasMessage("No pending inbox entry found")
        }

        @Test
        fun problemReportCantBeImported() {
            val command = createInboxCommand1()
            val inboxEntry1 = createInboxEntry1().copy(
                problemReportType = ProblemReportType.OTHER
            )
            every { inboxPort.findById(INBOX_ENTRY1_ID) } returns inboxEntry1
            assertThatThrownBy { inboxService.importPhoto(command) }.isInstanceOf(
                IllegalArgumentException::class.java
            ).hasMessage("No photo to import")
        }

        @Test
        fun stationNotFound() {
            val command = createInboxCommand1()
            every { inboxPort.findById(INBOX_ENTRY1_ID) } returns createInboxEntry1()

            assertThatThrownBy { inboxService.importPhoto(command) }.isInstanceOf(
                IllegalArgumentException::class.java
            ).hasMessage("Station not found")
        }

        @Test
        fun stationHasPhotoAndNoConflictResolutionProvided() {
            val command = createInboxCommand1()
            every { inboxPort.findById(INBOX_ENTRY1_ID) } returns createInboxEntry1()
            whenStation1ExistsWithPhoto()

            assertThatThrownBy { inboxService.importPhoto(command) }.isInstanceOf(
                IllegalArgumentException::class.java
            ).hasMessage("There is a conflict with another photo")
        }

        @Test
        fun stationHasNoPhotoButAnotherUploadsForThisStationExistsAndNoConflictResolutionProvided() {
            val command = createInboxCommand1()
            val inboxEntry = createInboxEntry1()
            every { inboxPort.findById(INBOX_ENTRY1_ID) } returns inboxEntry
            every {
                inboxPort.countPendingInboxEntriesForStation(
                    INBOX_ENTRY1_ID,
                    inboxEntry.countryCode!!,
                    inboxEntry.stationId!!
                )
            } returns 1
            every { stationPort.findByKey(STATION_KEY_DE_1.country, STATION_KEY_DE_1.id) } returns createStationDe1()

            assertThatThrownBy { inboxService.importPhoto(command) }.isInstanceOf(
                IllegalArgumentException::class.java
            ).hasMessage("There is a conflict with another photo")
        }

        @Test
        fun stationHasNoPhotoButAnotherUploadsForThisStationExistsAndWrongConflictResolutionProvided() {
            val command = createInboxCommand1().copy(
                conflictResolution = InboxCommand.ConflictResolution.OVERWRITE_EXISTING_PHOTO
            )
            val inboxEntry = createInboxEntry1()
            every { inboxPort.findById(INBOX_ENTRY1_ID) } returns inboxEntry
            every {
                inboxPort.countPendingInboxEntriesForStation(
                    INBOX_ENTRY1_ID,
                    inboxEntry.countryCode!!,
                    inboxEntry.stationId!!
                )
            } returns 1
            every { stationPort.findByKey(STATION_KEY_DE_1.country, STATION_KEY_DE_1.id) } returns createStationDe1()

            assertThatThrownBy { inboxService.importPhoto(command) }.isInstanceOf(
                IllegalArgumentException::class.java
            )
                .hasMessage("Conflict with another upload! The only possible ConflictResolution strategy is IMPORT_AS_NEW_PRIMARY_PHOTO.")
        }
    }

    private fun assertPhotoCapture(id: Long, stationKeyDe1: Station.Key, primary: Boolean) {
        assertThat(photoCaptor.captured).usingRecursiveComparison().ignoringFields("createdAt")
            .isEqualTo(
                Photo(
                    id = id,
                    stationKey = stationKeyDe1,
                    primary = primary,
                    urlPath = IMPORTED_PHOTO_URL_PATH,
                    photographer = PHOTOGRAPHER,
                    createdAt = Instant.now(),
                    license = PHOTOGRAPHER.license,
                )
            )
    }

    private fun whenStation1ExistsWithPhoto() {
        var stationDe1 = createStationDe1()
        stationDe1 = stationDe1.copy(
            photos = listOf(
                Photo(
                    id = EXISTING_PHOTO_ID,
                    stationKey = stationDe1.key,
                    primary = true,
                    urlPath = "",
                    photographer = createValidUser(),
                    createdAt = Instant.now(),
                    license = License.CC0_10,
                    outdated = false
                )
            )
        )
        every { stationPort.findByKey(STATION_KEY_DE_1.country, STATION_KEY_DE_1.id) } returns stationDe1
    }

    private fun whenStation1Exists() {
        every { stationPort.findByKey(STATION_KEY_DE_1.country, STATION_KEY_DE_1.id) } returns createStationDe1()
    }

    private fun createNewStationByCommand(command: InboxCommand, stationId: String): Station {
        return Station(
            key = Station.Key(command.countryCode!!, stationId),
            title = command.title!!,
            coordinates = command.coordinates!!,
            ds100 = command.ds100,
            active = command.active!!
        )
    }

    private fun createNewStationCommand1(): InboxCommand = createInboxCommand1().copy(
        countryCode = DE.code,
        stationId = NEW_STATION_ID,
        title = NEW_STATION_TITLE,
        coordinates = NEW_COORDINATES,
        active = true,
    )

    private fun createStationDe1(): Station = Station(
        key = STATION_KEY_DE_1,
        title = "Station DE 1",
    )

    private fun createInboxCommand1(): InboxCommand = InboxCommand(
        id = INBOX_ENTRY1_ID,
        conflictResolution = InboxCommand.ConflictResolution.DO_NOTHING
    )

    private fun createInboxEntry1(): InboxEntry = InboxEntry(
        id = INBOX_ENTRY1_ID,
        countryCode = STATION_KEY_DE_1.country,
        stationId = STATION_KEY_DE_1.id,
        photographerId = PHOTOGRAPHER.id,
        extension = "jpg",
    )

    @Nested
    internal inner class ImportMissingStation {
        @Test
        fun importPhotoForNewStation() {
            val command = createNewStationCommand1()
            val inboxEntry = createInboxEntry1().copy(
                stationId = null
            )
            every { inboxPort.findById(INBOX_ENTRY1_ID) } returns inboxEntry
            every { stationPort.findByKey(STATION_KEY_DE_1.country, command.stationId!!) } returns null
            every { stationPort.maxZ } returns 1024
            val newStation = createNewStationByCommand(command, "Z1025")
            every { photoPort.insert(capture(photoCaptor)) } returns IMPORTED_PHOTO_ID
            every { photoStoragePort.importPhoto(inboxEntry, newStation) } returns IMPORTED_PHOTO_URL_PATH

            inboxService.importMissingStation(command)

            verify { inboxPort.countPendingInboxEntriesForNearbyCoordinates(command.id, command.coordinates!!) }
            verify { stationPort.countNearbyCoordinates(command.coordinates!!) }
            verify { stationPort.insert(newStation) }
            assertPhotoCapture(NEW_PHOTO_ID, newStation.key, true)
            verify { photoStoragePort.importPhoto(inboxEntry, newStation) }
            verify {
                inboxPort.updateMissingStationImported(
                    inboxEntry.id,
                    newStation.key.country,
                    newStation.key.id,
                    NEW_STATION_TITLE
                )
            }
        }

        @Test
        fun createNewStationWithoutPhoto() {
            val command = createNewStationCommand1()
            val inboxEntry = createInboxEntry1().copy(
                stationId = null,
                extension = null,
            )
            every { inboxPort.findById(INBOX_ENTRY1_ID) } returns inboxEntry
            every { stationPort.findByKey(STATION_KEY_DE_1.country, command.stationId!!) } returns null
            every { stationPort.maxZ } returns 4711
            val newStation = createNewStationByCommand(command, "Z4712")

            inboxService.importMissingStation(command)

            verify { inboxPort.countPendingInboxEntriesForNearbyCoordinates(command.id, command.coordinates!!) }
            verify { stationPort.countNearbyCoordinates(command.coordinates!!) }
            verify { stationPort.insert(newStation) }
            verify(exactly = 0) { photoPort.insert(any()) }
            verify(exactly = 0) { photoStoragePort.importPhoto(any(), any()) }
            verify {
                inboxPort.updateMissingStationImported(
                    inboxEntry.id,
                    newStation.key.country,
                    newStation.key.id,
                    NEW_STATION_TITLE
                )
            }
        }

        @Test
        fun noInboxEntryFound() {
            val command = createInboxCommand1()

            assertThatThrownBy { inboxService.importMissingStation(command) }.isInstanceOf(
                IllegalArgumentException::class.java
            ).hasMessage("No pending inbox entry found")
        }

        @Test
        fun noPendingInboxEntryFound() {
            val command = createInboxCommand1()
            val inboxEntry1 = createInboxEntry1().copy(
                done = true
            )
            every { inboxPort.findById(INBOX_ENTRY1_ID) } returns inboxEntry1

            assertThatThrownBy { inboxService.importMissingStation(command) }.isInstanceOf(
                IllegalArgumentException::class.java
            ).hasMessage("No pending inbox entry found")
        }

        @Test
        fun problemReportCantBeImported() {
            val command = createInboxCommand1()
            val inboxEntry1 = createInboxEntry1().copy(
                problemReportType = ProblemReportType.OTHER
            )
            every { inboxPort.findById(INBOX_ENTRY1_ID) } returns inboxEntry1

            assertThatThrownBy { inboxService.importMissingStation(command) }.isInstanceOf(
                IllegalArgumentException::class.java
            ).hasMessage("Can't import a problem report")
        }

        @Test
        fun stationNotFoundAndNotCreatedBecauseCountryNotFound() {
            val command = createNewStationCommand1().copy(
                countryCode = "xx"
            )
            every { inboxPort.findById(INBOX_ENTRY1_ID) } returns createInboxEntry1()

            assertThatThrownBy { inboxService.importMissingStation(command) }.isInstanceOf(
                IllegalArgumentException::class.java
            ).hasMessage("Country not found")
        }

        @Test
        fun stationNotFoundAndNotCreatedBecauseNoValidCoordinatesProvides() {
            val command = createNewStationCommand1().copy(
                coordinates = Coordinates(500.0, -300.0)
            )
            every { inboxPort.findById(INBOX_ENTRY1_ID) } returns createInboxEntry1()

            assertThatThrownBy { inboxService.importMissingStation(command) }.isInstanceOf(
                IllegalArgumentException::class.java
            ).hasMessage("No valid coordinates provided")
        }

        @Test
        fun stationNotFoundAndNotCreatedBecauseNoCoordinatesProvides() {
            val command = createNewStationCommand1().copy(
                coordinates = null
            )
            every { inboxPort.findById(INBOX_ENTRY1_ID) } returns createInboxEntry1()

            assertThatThrownBy { inboxService.importMissingStation(command) }.isInstanceOf(
                IllegalArgumentException::class.java
            ).hasMessage("No valid coordinates provided")
        }

        @Test
        fun stationNotFoundAndNotCreatedBecauseTitleIsEmpty() {
            val command = createNewStationCommand1().copy(
                title = null
            )
            every { inboxPort.findById(INBOX_ENTRY1_ID) } returns createInboxEntry1()

            assertThatThrownBy { inboxService.importMissingStation(command) }.isInstanceOf(
                IllegalArgumentException::class.java
            ).hasMessage("Station title can't be empty")
        }

        @Test
        fun stationNotFoundAndNotCreatedBecauseNoActiveFlagProvided() {
            val command = createNewStationCommand1().copy(
                active = null
            )
            every { inboxPort.findById(INBOX_ENTRY1_ID) } returns createInboxEntry1()

            assertThatThrownBy { inboxService.importMissingStation(command) }.isInstanceOf(
                IllegalArgumentException::class.java
            ).hasMessage("No Active flag provided")
        }

        @Test
        fun stationHasPhotoAndNoConflictResolutionProvided() {
            val command = createInboxCommand1().copy(
                countryCode = STATION_KEY_DE_1.country,
                stationId = STATION_KEY_DE_1.id,
            )
            every { inboxPort.findById(INBOX_ENTRY1_ID) } returns createInboxEntry1()
            whenStation1ExistsWithPhoto()

            assertThatThrownBy { inboxService.importMissingStation(command) }.isInstanceOf(
                IllegalArgumentException::class.java
            ).hasMessage("There is a conflict with another photo")
        }

        @Test
        fun stationHasNoPhotoButAnotherUploadsForThisStationExistsAndNoConflictResolutionProvided() {
            val command = createInboxCommand1().copy(
                countryCode = STATION_KEY_DE_1.country,
                stationId = STATION_KEY_DE_1.id,
            )
            val inboxEntry = createInboxEntry1()
            every { inboxPort.findById(INBOX_ENTRY1_ID) } returns inboxEntry
            every {
                inboxPort.countPendingInboxEntriesForStation(
                    INBOX_ENTRY1_ID,
                    inboxEntry.countryCode!!,
                    inboxEntry.stationId!!
                )
            } returns 1
            every { stationPort.findByKey(STATION_KEY_DE_1.country, STATION_KEY_DE_1.id) } returns createStationDe1()

            assertThatThrownBy { inboxService.importMissingStation(command) }.isInstanceOf(
                IllegalArgumentException::class.java
            ).hasMessage("There is a conflict with another photo")
        }
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    internal inner class UserInbox {
        @Test
        fun deleteUserInboxEntry() {
            val inboxEntry = createInboxEntry1()
            every { inboxPort.findById(inboxEntry.id) } returns inboxEntry
            every { inboxPort.reject(any(), any()) } returns Unit

            inboxService.deleteUserInboxEntry(PHOTOGRAPHER, inboxEntry.id)

            verify { inboxPort.reject(inboxEntry.id, "Withdrawn by user") }
            verify { monitorPort.sendMessage("InboxEntry ${inboxEntry.id} ${inboxEntry.title} has been withdrawn by ${PHOTOGRAPHER.name}") }
        }

        @Test
        fun deleteUserInboxEntryNotOwner() {
            val inboxEntry = createInboxEntry1()
            every { inboxPort.findById(inboxEntry.id) } returns inboxEntry

            assertThatThrownBy { inboxService.deleteUserInboxEntry(createValidUser(), 1L) }
                .isInstanceOf(ManageInboxUseCase.InboxEntryNotOwnerException::class.java)

            verify(exactly = 0) { inboxPort.reject(inboxEntry.id, any()) }
        }

        @Test
        fun deleteUserInboxEntryAlreadyDone() {
            val inboxEntry = createInboxEntry1().copy(
                done = true
            )
            every { inboxPort.findById(inboxEntry.id) } returns inboxEntry

            assertThatThrownBy { inboxService.deleteUserInboxEntry(PHOTOGRAPHER, 1L) }
                .isInstanceOf(IllegalArgumentException::class.java)

            verify(exactly = 0) { inboxPort.reject(inboxEntry.id, any()) }
        }

        @Test
        fun deleteUserInboxEntryNotFound() {
            every { inboxPort.findById(1L) } returns null

            assertThatThrownBy { inboxService.deleteUserInboxEntry(PHOTOGRAPHER, 1L) }
                .isInstanceOf(ManageInboxUseCase.InboxEntryNotFoundException::class.java)
        }
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    internal inner class ReportProblem {
        @Test
        fun reportWrongPhoto() {
            val problemReport = createWrongPhotoProblemReport(EXISTING_PHOTO_ID)
            whenStation1ExistsWithPhoto()

            val inboxResponse = inboxService.reportProblem(problemReport, createValidUser(), null)

            assertThat(inboxResponse.state).isEqualTo(InboxResponseState.REVIEW)
        }

        @Test
        fun reportWrongPhotoWithWrongPhotoId() {
            val problemReport = createWrongPhotoProblemReport(0L)
            whenStation1ExistsWithPhoto()

            val inboxResponse = inboxService.reportProblem(problemReport, createValidUser(), null)

            assertThat(inboxResponse.state).isEqualTo(InboxResponseState.NOT_ENOUGH_DATA)
            assertThat(inboxResponse.message).isEqualTo("Photo with this id not found at station")
        }

        @ParameterizedTest
        @MethodSource("invalidUsersForReportProblem")
        fun reportProblemWithInvalidUser(invalidUser: User?) {
            val problemReport = createWrongPhotoProblemReport(EXISTING_PHOTO_ID)

            val inboxResponse = inboxService.reportProblem(problemReport, invalidUser!!, null)

            assertThat(inboxResponse.state).isEqualTo(InboxResponseState.UNAUTHORIZED)
            assertThat(inboxResponse.message).isEqualTo("Profile incomplete")
        }

        @Test
        fun reportWrongPhotoForStationWithoutPhoto() {
            val problemReport = createWrongPhotoProblemReport(EXISTING_PHOTO_ID)
            whenStation1Exists()

            val inboxResponse = inboxService.reportProblem(problemReport, createValidUser(), null)

            assertThat(inboxResponse.state).isEqualTo(InboxResponseState.NOT_ENOUGH_DATA)
            assertThat(inboxResponse.message)
                .isEqualTo("Problem type is only applicable to station with photo")
        }

        private fun createWrongPhotoProblemReport(existingPhotoId: Long): ProblemReport {
            return ProblemReport(
                countryCode = STATION_KEY_DE_1.country,
                stationId = STATION_KEY_DE_1.id,
                photoId = existingPhotoId,
                type = ProblemReportType.WRONG_PHOTO,
                comment = "a comment",
            )
        }

        @Test
        fun changeNameCommand() {
            whenStation1Exists()
            whenWrongNameProblemReportExists()

            inboxService.changeStationTitle(
                InboxCommand(
                    id = INBOX_ENTRY1_ID,
                    title = "New Title By Admin",
                )
            )

            verify { stationPort.changeStationTitle(STATION_KEY_DE_1, "New Title By Admin") }
            verify { inboxPort.done(INBOX_ENTRY1_ID) }
        }

        @Test
        fun changeNameCommandWithoutTitle() {
            whenWrongNameProblemReportExists()

            assertThatThrownBy {
                inboxService.changeStationTitle(
                    InboxCommand(
                        id = INBOX_ENTRY1_ID,
                    )
                )
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Empty new title: null")
        }

        private fun whenWrongNameProblemReportExists() {
            every { inboxPort.findById(INBOX_ENTRY1_ID) } returns InboxEntry(
                id = INBOX_ENTRY1_ID,
                countryCode = STATION_KEY_DE_1.country,
                stationId = STATION_KEY_DE_1.id,
                title = "Old Title",
                newTitle = "New Title By Reporter",
                problemReportType = ProblemReportType.WRONG_NAME,
            )
        }

        @Test
        fun changeLocationCommandWithCoordinatesByAdmin() {
            whenStation1Exists()
            whenWrongLocationInboxEntryExists()

            inboxService.updateLocation(
                InboxCommand(
                    id = INBOX_ENTRY1_ID,
                    coordinates = Coordinates(52.0, 11.0),
                )
            )

            verify { stationPort.updateLocation(STATION_KEY_DE_1, Coordinates(52.0, 11.0)) }
            verify { inboxPort.done(INBOX_ENTRY1_ID) }
        }

        private fun whenWrongLocationInboxEntryExists() {
            every { inboxPort.findById(INBOX_ENTRY1_ID) } returns
                    InboxEntry(
                        id = INBOX_ENTRY1_ID,
                        countryCode = STATION_KEY_DE_1.country,
                        stationId = STATION_KEY_DE_1.id,
                        coordinates = Coordinates(50.0, 9.0),
                        newCoordinates = Coordinates(51.0, 10.0),
                        problemReportType = ProblemReportType.WRONG_LOCATION,
                    )
        }

        @Test
        fun changeLocationCommandWithoutNewCoordinatesByAdmin() {
            whenWrongLocationInboxEntryExists()

            assertThatThrownBy {
                inboxService.updateLocation(
                    InboxCommand(
                        id = INBOX_ENTRY1_ID,
                    )
                )
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Can't update location, coordinates: null")
        }

        @Test
        fun changeLocationCommandWithZeroCoordinatesByAdmin() {
            whenWrongLocationInboxEntryExists()

            assertThatThrownBy {
                inboxService.updateLocation(
                    InboxCommand(
                        id = INBOX_ENTRY1_ID,
                        coordinates = Coordinates()
                    )
                )
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Can't update location, coordinates: Coordinates(lat=0.0, lon=0.0)")
        }

        @Test
        fun changeLocationCommandWithInvalidCoordinatesByAdmin() {
            whenWrongLocationInboxEntryExists()

            assertThatThrownBy {
                inboxService.updateLocation(
                    InboxCommand(
                        id = INBOX_ENTRY1_ID,
                        coordinates = Coordinates(110.0, 90.0),
                    )
                )
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Can't update location, coordinates: Coordinates(lat=110.0, lon=90.0)")
        }

        @Test
        fun deleteStation() {
            val inboxEntry1 = InboxEntry(
                id = INBOX_ENTRY1_ID,
                countryCode = STATION_KEY_DE_1.country,
                stationId = STATION_KEY_DE_1.id,
                problemReportType = ProblemReportType.DUPLICATE,
            )
            every { inboxPort.findById(inboxEntry1.id) } returns inboxEntry1
            every {
                stationPort.findByKey(
                    inboxEntry1.countryCode!!,
                    inboxEntry1.stationId!!
                )
            } returns createStationDe1()
            val inboxEntry2 = InboxEntry(
                id = 2,
                countryCode = "de",
                stationId = "4711",
            )
            every { inboxPort.findById(inboxEntry2.id) } returns inboxEntry2
            every { inboxPort.findPendingByStation(inboxEntry1.countryCode!!, inboxEntry1.stationId!!) } returns listOf(
                inboxEntry2
            )
            every { stationPort.delete(any()) } returns Unit
            every { inboxPort.reject(any(), any()) } returns Unit
            every { photoStoragePort.reject(any()) } returns Unit

            inboxService.deleteStation(
                InboxCommand(
                    id = inboxEntry1.id,
                    countryCode = inboxEntry1.countryCode,
                    stationId = inboxEntry1.stationId,
                )
            )

            verify { stationPort.delete(STATION_KEY_DE_1) }
            verify { inboxPort.done(inboxEntry1.id) }
            verify { inboxPort.reject(inboxEntry2.id, "Station has been deleted") }
            verify { photoStoragePort.reject(inboxEntry2) }
        }


        private fun invalidUsersForReportProblem(): List<Arguments> {
            val userWithoutName = createValidUser().copy(
                name = ""
            )
            val userWithoutEmail = createValidUser().copy(
                email = null
            )
            val userWithEmailVerificationToken = createValidUser().copy(
                emailVerification = "SOME_TOKEN"
            )
            return listOf(
                Arguments.of(userWithoutName),
                Arguments.of(userWithoutEmail),
                Arguments.of(userWithEmailVerificationToken)
            )
        }
    }

    companion object {
        val DE: Country = Country(
            code = "de",
            name = "Germany",
            email = "email@example.com",
        )
        val STATION_KEY_DE_1: Station.Key = Station.Key(DE.code, "1")
        const val INBOX_ENTRY1_ID: Long = 1
        val PHOTOGRAPHER: User = User(
            id = 1,
            name = "nickname",
            license = License.CC0_10,
            ownPhotos = true,
        )
        const val EXISTING_PHOTO_ID: Long = 1L
        const val IMPORTED_PHOTO_ID: Long = 2L
        const val IMPORTED_PHOTO_URL_PATH: String = "/de/1.jpg"
        val NEW_COORDINATES: Coordinates = Coordinates(1.0, 2.0)
        const val NEW_STATION_ID: String = "Z1"
        const val NEW_STATION_TITLE: String = "New Station"
        const val NEW_PHOTO_ID: Long = 0L

        fun createCountryWithOverrideLicense(overrideLicense: License?): Country {
            return Country(
                code = "xx",
                name = "XX",
                email = "email@example.com",
                timetableUrlTemplate = null,
                overrideLicense = overrideLicense,
            )
        }

        fun createValidUser(): User {
            return User(
                name = "name",
                license = License.CC0_10,
                email = "email@example.com",
                ownPhotos = true,
                emailVerification = User.EMAIL_VERIFIED,
            )
        }
    }
}