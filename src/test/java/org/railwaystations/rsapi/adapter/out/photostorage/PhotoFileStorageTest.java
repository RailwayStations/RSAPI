package org.railwaystations.rsapi.adapter.out.photostorage;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.railwaystations.rsapi.core.model.InboxEntry;
import org.railwaystations.rsapi.core.model.Station;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class PhotoFileStorageTest {

    static final int KEEP_FILE_COPIES_IN_DAYS = 90;

    Path tempdir;

    PhotoFileStorage storage;
    WorkDir workDir;

    @BeforeEach
    void setup() throws IOException {
        tempdir = Files.createTempDirectory("rsapi");
        workDir = new WorkDir(tempdir.toString(), KEEP_FILE_COPIES_IN_DAYS);
        storage = new PhotoFileStorage(workDir);
    }

    @Test
    void sanitizeFilename() {
        assertThat(PhotoFileStorage.sanitizeFilename("../../../s*me\\\\very\\<evil>*/file:name?")).isEqualTo(".._.._.._s_me__very__evil___file_name_");
    }

    @ParameterizedTest
    @ValueSource(strings = {"done", "rejected"})
    void cleanupOldCopies(String subdirName) throws IOException {
        var subdir = workDir.getInboxDir().resolve(subdirName);
        var newFile = createFileWithLastModifiedInPast(subdir, "newFile.txt", KEEP_FILE_COPIES_IN_DAYS - 1);
        var oldFile = createFileWithLastModifiedInPast(subdir, "oldFile.txt", KEEP_FILE_COPIES_IN_DAYS + 1);

        storage.cleanupOldCopies();

        assertThat(Files.exists(newFile)).isEqualTo(true);
        assertThat(Files.exists(oldFile)).isEqualTo(false);
    }

    @NotNull
    private Path createFileWithLastModifiedInPast(Path subdir, String filename, int lastModifiedDaysInPast) throws IOException {
        var path = createFile(subdir, filename);
        Files.setLastModifiedTime(path, FileTime.from(Instant.now().minus(lastModifiedDaysInPast, ChronoUnit.DAYS)));
        return path;
    }

    @NotNull
    private Path createFile(Path subdir, String filename) throws IOException {
        var path = subdir.resolve(filename);
        Files.createDirectories(subdir);
        Files.writeString(path, filename);
        return path;
    }

    @Test
    void importPhoto() throws IOException {
        var stationKey = new Station.Key("de", "4711");
        var station = Station.builder()
                .key(stationKey)
                .build();
        var inboxEntry = createInboxEntryWithId(1, stationKey);
        createFile(workDir.getInboxDir(), inboxEntry.getFilename());
        createFile(workDir.getInboxProcessedDir(), inboxEntry.getFilename());

        var urlPath = storage.importPhoto(inboxEntry, station);

        assertThat(urlPath).isEqualTo("/de/4711_1.jpg");
        assertThat(workDir.getPhotosDir().resolve("de").resolve("4711_1.jpg")).exists();
        assertThat(workDir.getInboxDoneDir().resolve(inboxEntry.getFilename())).exists();
        assertThat(workDir.getInboxDir().resolve(inboxEntry.getFilename())).doesNotExist();
        assertThat(workDir.getInboxProcessedDir().resolve(inboxEntry.getFilename())).doesNotExist();
    }

    @Test
    void importSecondPhoto() throws IOException {
        var stationKey = new Station.Key("de", "0815");
        var station = Station.builder()
                .key(stationKey)
                .build();
        var inboxEntry = createInboxEntryWithId(2, stationKey);
        createFile(workDir.getInboxDir(), inboxEntry.getFilename());
        createFile(workDir.getInboxProcessedDir(), inboxEntry.getFilename());
        createFile(workDir.getPhotosDir().resolve("de"), "0815_1.jpg");

        var urlPath = storage.importPhoto(inboxEntry, station);

        assertThat(urlPath).isEqualTo("/de/0815_2.jpg");
        assertThat(workDir.getPhotosDir().resolve("de").resolve("0815_2.jpg")).exists();
        assertThat(workDir.getInboxDoneDir().resolve(inboxEntry.getFilename())).exists();
        assertThat(workDir.getInboxDir().resolve(inboxEntry.getFilename())).doesNotExist();
        assertThat(workDir.getInboxProcessedDir().resolve(inboxEntry.getFilename())).doesNotExist();
    }

    private InboxEntry createInboxEntryWithId(int id, Station.Key stationKey) {
        return InboxEntry.builder()
                .id(id)
                .countryCode(stationKey.getCountry())
                .stationId(stationKey.getId())
                .extension("jpg")
                .build();
    }

}