package org.railwaystations.rsapi.adapter.photostorage

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.railwaystations.rsapi.adapter.photostorage.PhotoFileStorage.Companion.sanitizeFilename
import org.railwaystations.rsapi.core.model.InboxEntry
import org.railwaystations.rsapi.core.model.Station
import org.railwaystations.rsapi.core.model.StationTestFixtures
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.time.temporal.ChronoUnit

const val KEEP_FILE_COPIES_IN_DAYS: Int = 90

internal class PhotoFileStorageTest {
    private lateinit var tempdir: Path
    private lateinit var storage: PhotoFileStorage
    private lateinit var workDir: WorkDir

    @BeforeEach
    @Throws(IOException::class)
    fun setup() {
        tempdir = Files.createTempDirectory("rsapi")
        workDir = WorkDir(tempdir.toString(), KEEP_FILE_COPIES_IN_DAYS)
        storage = PhotoFileStorage(workDir)
    }

    @Test
    fun sanitizeFilename() {
        assertThat(sanitizeFilename("../../../s*me\\\\very\\<evil>*/file:name?"))
            .isEqualTo(".._.._.._s_me__very__evil___file_name_")
    }

    @ParameterizedTest
    @ValueSource(strings = ["done", "rejected"])
    @Throws(IOException::class)
    fun cleanupOldCopies(subdirName: String) {
        val subdir = workDir.inboxDir.resolve(subdirName)
        val newFile = createFileWithLastModifiedInPast(subdir, "newFile.txt", KEEP_FILE_COPIES_IN_DAYS - 1)
        val oldFile = createFileWithLastModifiedInPast(subdir, "oldFile.txt", KEEP_FILE_COPIES_IN_DAYS + 1)

        storage.cleanupOldCopies()

        assertThat(Files.exists(newFile)).isEqualTo(true)
        assertThat(Files.exists(oldFile)).isEqualTo(false)
    }

    @Throws(IOException::class)
    private fun createFileWithLastModifiedInPast(subdir: Path, filename: String, lastModifiedDaysInPast: Int): Path {
        val path = createFile(subdir, filename)
        Files.setLastModifiedTime(
            path,
            FileTime.from(Instant.now().minus(lastModifiedDaysInPast.toLong(), ChronoUnit.DAYS))
        )
        return path
    }

    @Throws(IOException::class)
    private fun createFile(subdir: Path, filename: String): Path {
        val path = subdir.resolve(filename)
        Files.createDirectories(subdir)
        Files.writeString(path, filename)
        return path
    }

    @Test
    @Throws(IOException::class)
    fun importPhoto() {
        val station = StationTestFixtures.createStationDe4711()
        val inboxEntry = createInboxEntryWithId(1, station.key)
        val filename = inboxEntry.filename!!
        createFile(workDir.inboxDir, filename)
        createFile(workDir.inboxProcessedDir, filename)

        val urlPath = storage.importPhoto(inboxEntry, station)

        assertThat(urlPath).isEqualTo("/de/4711_1.jpg")
        assertThat(workDir.photosDir.resolve("de").resolve("4711_1.jpg")).exists()
        assertThat(workDir.inboxDoneDir.resolve(filename)).exists()
        assertThat(workDir.inboxDir.resolve(filename)).doesNotExist()
        assertThat(workDir.inboxProcessedDir.resolve(filename)).doesNotExist()
    }

    @Test
    @Throws(IOException::class)
    fun importSecondPhoto() {
        val station = StationTestFixtures.createStationDe0815()
        val inboxEntry = createInboxEntryWithId(2, station.key)
        val filename = inboxEntry.filename!!
        createFile(workDir.inboxDir, filename)
        createFile(workDir.inboxProcessedDir, filename)
        createFile(workDir.photosDir.resolve("de"), "0815_1.jpg")

        val urlPath = storage.importPhoto(inboxEntry, station)

        assertThat(urlPath).isEqualTo("/de/0815_2.jpg")
        assertThat(workDir.photosDir.resolve("de").resolve("0815_2.jpg")).exists()
        assertThat(workDir.inboxDoneDir.resolve(filename)).exists()
        assertThat(workDir.inboxDir.resolve(filename)).doesNotExist()
        assertThat(workDir.inboxProcessedDir.resolve(filename)).doesNotExist()
    }

    private fun createInboxEntryWithId(id: Int, key: Station.Key): InboxEntry {
        return InboxEntry(
            id = id.toLong(),
            countryCode = key.country,
            stationId = key.id,
            extension = "jpg",
        )
    }

}