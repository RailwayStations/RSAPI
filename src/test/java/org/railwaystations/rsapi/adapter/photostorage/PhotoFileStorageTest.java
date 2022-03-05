package org.railwaystations.rsapi.adapter.photostorage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class PhotoFileStorageTest {

    @Test
    void sanitizeFilename() {
        assertThat(PhotoFileStorage.sanitizeFilename("../../../s*me\\\\very\\<evil>*/file:name?"), is(".._.._.._s_me__very__evil___file_name_"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"done", "rejected"})
    void cleanupOldCopies(final String subdirName) throws IOException {
        final Path tempdir = Files.createTempDirectory("rsapi");
        final PhotoFileStorage storage = new PhotoFileStorage(new WorkDir(tempdir.toString(), 90));
        final Path subdir = tempdir.resolve("inbox").resolve(subdirName);
        Files.createDirectories(subdir);
        final Path newFile = subdir.resolve("newFile.txt");
        Files.writeString(newFile, "newFile");
        Files.setLastModifiedTime(newFile, FileTime.from(Instant.now().minus(89, ChronoUnit.DAYS)));
        final Path oldFile = subdir.resolve("oldFile.txt");
        Files.writeString(oldFile, "oldFile");
        Files.setLastModifiedTime(oldFile, FileTime.from(Instant.now().minus(91, ChronoUnit.DAYS)));

        storage.cleanupOldCopies();

        assertThat(Files.exists(newFile), is(true));
        assertThat(Files.exists(oldFile), is(false));
    }
}