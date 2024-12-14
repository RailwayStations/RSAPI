package org.railwaystations.rsapi.adapter.db

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.railwaystations.rsapi.core.model.Coordinates
import org.railwaystations.rsapi.core.model.InboxEntryTestFixtures.photoUploadDe8000
import org.railwaystations.rsapi.core.model.InboxEntryTestFixtures.photoUploadMissingStation
import org.railwaystations.rsapi.core.model.InboxEntryTestFixtures.problemReportDe8000
import org.railwaystations.rsapi.core.model.PublicInboxEntry
import org.railwaystations.rsapi.core.model.StationTestFixtures.stationDe8000
import org.railwaystations.rsapi.core.model.StationTestFixtures.stationDe8001
import org.railwaystations.rsapi.core.model.UserTestFixtures.user10
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import java.time.Instant
import java.time.temporal.ChronoUnit

@JooqTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(
    JooqCustomizerConfiguration::class,
    InboxAdapter::class,
)
class InboxAdapterTest : AbstractPostgreSqlTest() {

    @Autowired
    private lateinit var sut: InboxAdapter

    @Test
    fun insertAndFindById() {
        val inboxEntry = photoUploadDe8000
        val id = sut.insert(inboxEntry)

        val inboxDbEntry = sut.findById(id)

        assertThat(inboxDbEntry).isEqualTo(
            inboxEntry.copy(
                id = id,
                title = stationDe8000.title,
                coordinates = stationDe8000.coordinates,
                photographerNickname = user10.name,
                photographerEmail = user10.email,
            )
        )
    }

    @Test
    fun findPendingInboxEntries() {
        val idPending = sut.insert(photoUploadDe8000.copy(done = false))
        sut.insert(photoUploadDe8000.copy(done = true))

        val pendingEntries = sut.findPendingInboxEntries()

        assertThat(pendingEntries.map { it.id }).containsExactly(idPending)
    }

    @Test
    fun findOldestImportedPhotoNotYetPosted() {
        val now = Instant.now()
        sut.insert(photoUploadDe8000.copy(done = true, posted = true, createdAt = now.minus(10, ChronoUnit.HOURS)))
        sut.insert(photoUploadDe8000.copy(done = true, posted = false, createdAt = now.minus(5, ChronoUnit.HOURS)))
        val idNotPostedOlder =
            sut.insert(photoUploadDe8000.copy(done = true, posted = false, createdAt = now.minus(8, ChronoUnit.HOURS)))

        val oldestNotYetPosted = sut.findOldestImportedPhotoNotYetPosted()

        assertThat(oldestNotYetPosted!!.id).isEqualTo(idNotPostedOlder)
    }

    @Test
    fun findPublicInboxEntries() {
        sut.insert(photoUploadDe8000)
        sut.insert(photoUploadDe8000.copy(done = true, stationId = stationDe8001.key.id))
        sut.insert(problemReportDe8000)

        val publicInboxEntries = sut.findPublicInboxEntries()

        assertThat(publicInboxEntries).containsExactly(
            PublicInboxEntry(
                countryCode = photoUploadDe8000.countryCode,
                stationId = photoUploadDe8000.stationId,
                title = stationDe8000.title,
                coordinates = stationDe8000.coordinates,
            )
        )
    }

    @Test
    fun reject() {
        val id = sut.insert(photoUploadDe8000.copy(rejectReason = null, done = false))

        sut.reject(id, "reject reason")

        val actual = sut.findById(id)
        assertThat(actual!!.done).isTrue
        assertThat(actual.rejectReason).isEqualTo("reject reason")
    }

    @Test
    fun done() {
        val id = sut.insert(photoUploadDe8000.copy(rejectReason = null, done = false))

        sut.done(id)

        val actual = sut.findById(id)
        assertThat(actual!!.done).isTrue
        assertThat(actual.rejectReason).isNull()
    }

    @Test
    fun countPendingInboxEntriesForStationWithoutInboxId() {
        sut.insert(photoUploadDe8000)
        sut.insert(photoUploadDe8000.copy(stationId = stationDe8001.key.id))
        sut.insert(photoUploadDe8000.copy(done = true))
        sut.insert(problemReportDe8000)
        sut.insert(problemReportDe8000.copy(stationId = stationDe8001.key.id))

        val count = sut.countPendingInboxEntriesForStation(null, stationDe8000.key.country, stationDe8000.key.id)

        assertThat(count).isEqualTo(2)
    }

    @Test
    fun countPendingInboxEntriesForStationWithInboxId() {
        val id1 = sut.insert(photoUploadDe8000)
        sut.insert(photoUploadDe8000.copy(stationId = stationDe8001.key.id))
        sut.insert(photoUploadDe8000.copy(done = true))
        sut.insert(problemReportDe8000)
        sut.insert(problemReportDe8000.copy(stationId = stationDe8001.key.id))

        val count = sut.countPendingInboxEntriesForStation(id1, stationDe8000.key.country, stationDe8000.key.id)

        assertThat(count).isEqualTo(1)
    }

    @Test
    fun countPendingInboxEntries() {
        sut.insert(photoUploadDe8000)
        sut.insert(photoUploadDe8000.copy(stationId = stationDe8001.key.id))
        sut.insert(photoUploadDe8000.copy(done = true))
        sut.insert(problemReportDe8000)
        sut.insert(problemReportDe8000.copy(stationId = stationDe8001.key.id))

        val count = sut.countPendingInboxEntries()

        assertThat(count).isEqualTo(4)
    }

    @Test
    fun countPendingInboxEntriesForNearbyCoordinatesWithoutInboxId() {
        sut.insert(photoUploadDe8000.copy(coordinates = Coordinates(50.10123, 9.50123)))
        sut.insert(photoUploadDe8000.copy(coordinates = Coordinates(50.09856, 9.49956)))
        sut.insert(photoUploadDe8000.copy(coordinates = Coordinates(60.1, 19.3)))

        val count = sut.countPendingInboxEntriesForNearbyCoordinates(null, Coordinates(50.10, 9.50))

        assertThat(count).isEqualTo(2)
    }

    @Test
    fun countPendingInboxEntriesForNearbyCoordinatesWithInboxId() {
        val id1 = sut.insert(photoUploadDe8000.copy(coordinates = Coordinates(50.10123, 9.50123)))
        sut.insert(photoUploadDe8000.copy(coordinates = Coordinates(50.09856, 9.49956)))
        sut.insert(photoUploadDe8000.copy(coordinates = Coordinates(60.1, 19.3)))

        val count = sut.countPendingInboxEntriesForNearbyCoordinates(id1, Coordinates(50.10, 9.50))

        assertThat(count).isEqualTo(1)
    }

    @Test
    fun updateCrc32() {
        val id = sut.insert(photoUploadDe8000.copy(crc32 = 1234))

        sut.updateCrc32(id, 45678)

        val actual = sut.findById(id)
        assertThat(actual!!.crc32).isEqualTo(45678)
    }

    @Test
    fun findInboxEntriesToNotify() {
        val id1 = sut.insert(photoUploadDe8000.copy(done = true, notified = false))
        sut.insert(photoUploadDe8000.copy(done = true, notified = true))
        sut.insert(photoUploadDe8000.copy(done = false, notified = false))

        val findsToNotify = sut.findInboxEntriesToNotify()

        assertThat(findsToNotify.size).isEqualTo(1)
        assertThat(findsToNotify.first().id).isEqualTo(id1)
    }

    @Test
    fun updateNotified() {
        val id = sut.insert(photoUploadDe8000.copy(notified = false))

        sut.updateNotified(listOf(id))

        val actual = sut.findById(id)
        assertThat(actual!!.notified).isTrue()
    }

    @Test
    fun updatePosted() {
        val id = sut.insert(photoUploadDe8000.copy(posted = false))

        sut.updatePosted(id)

        val actual = sut.findById(id)
        assertThat(actual!!.posted).isTrue()
    }

    @Test
    fun updatePhotoId() {
        val id = sut.insert(photoUploadDe8000.copy(photoId = null))

        sut.updatePhotoId(id, 1234)

        val actual = sut.findById(id)
        assertThat(actual!!.photoId).isEqualTo(1234)
    }

    @Test
    fun updateMissingStationImported() {
        val id = sut.insert(photoUploadMissingStation)

        sut.updateMissingStationImported(
            id = id,
            countryCode = stationDe8000.key.country,
            stationId = stationDe8000.key.id,
            title = stationDe8000.title,
        )

        val updatedEntry = sut.findById(id)
        assertThat(updatedEntry!!.countryCode).isEqualTo(stationDe8000.key.country)
        assertThat(updatedEntry.stationId).isEqualTo(stationDe8000.key.id)
        assertThat(updatedEntry.title).isEqualTo(stationDe8000.title)
        assertThat(updatedEntry.done).isTrue()
    }

    @Test
    fun findByUserIncludeCompletedEntries() {
        val id1 = sut.insert(photoUploadDe8000.copy(photographerId = 1, done = false))
        val id2 = sut.insert(photoUploadDe8000.copy(photographerId = 1, done = true))
        sut.insert(photoUploadDe8000.copy(photographerId = 2, done = true))

        val findsByUser = sut.findByUser(photographerId = 1, includeCompletedEntries = true)

        assertThat(findsByUser.map { it.id }).containsExactlyInAnyOrder(id1, id2)
    }

    @Test
    fun findByUserOnlyPendingEntries() {
        val id1 = sut.insert(photoUploadDe8000.copy(photographerId = 1, done = false))
        sut.insert(photoUploadDe8000.copy(photographerId = 1, done = true))
        sut.insert(photoUploadDe8000.copy(photographerId = 2, done = true))

        val findsByUser = sut.findByUser(photographerId = 1, includeCompletedEntries = false)

        assertThat(findsByUser.map { it.id }).containsExactlyInAnyOrder(id1)
    }

    @Test
    fun findPendingByStation() {
        val id1 = sut.insert(photoUploadDe8000)
        sut.insert(photoUploadDe8000.copy(stationId = stationDe8001.key.id))

        val findsByStation = sut.findPendingByStation(stationDe8000.key.country, stationDe8000.key.id)

        assertThat(findsByStation.size).isEqualTo(1)
        assertThat(findsByStation.first().id).isEqualTo(id1)
    }

}
