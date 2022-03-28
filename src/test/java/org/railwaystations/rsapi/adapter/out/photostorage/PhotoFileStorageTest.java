package org.railwaystations.rsapi.adapter.out.photostorage;

import org.jetbrains.annotations.NotNull;
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
        final int keepFileCopiesInDays = 90;
        final PhotoFileStorage storage = new PhotoFileStorage(new WorkDir(tempdir.toString(), keepFileCopiesInDays));
        final Path subdir = tempdir.resolve("inbox").resolve(subdirName);
        final Path newFile = createFileWithLastModifiedInPast(subdir, "newFile.txt", keepFileCopiesInDays - 1);
        final Path oldFile = createFileWithLastModifiedInPast(subdir, "oldFile.txt", keepFileCopiesInDays + 1);

        storage.cleanupOldCopies();

        assertThat(Files.exists(newFile), is(true));
        assertThat(Files.exists(oldFile), is(false));
    }

    @NotNull
    private Path createFileWithLastModifiedInPast(final Path subdir, final String filename, final int lastModifiedDaysInPast) throws IOException {
        final Path path = subdir.resolve(filename);
        Files.writeString(path, filename);
        Files.setLastModifiedTime(path, FileTime.from(Instant.now().minus(lastModifiedDaysInPast, ChronoUnit.DAYS)));
        return path;
    }

}