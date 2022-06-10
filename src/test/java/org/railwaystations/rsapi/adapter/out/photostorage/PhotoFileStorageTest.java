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

import static org.assertj.core.api.Assertions.assertThat;

class PhotoFileStorageTest {

    @Test
    void sanitizeFilename() {
        assertThat(PhotoFileStorage.sanitizeFilename("../../../s*me\\\\very\\<evil>*/file:name?")).isEqualTo(".._.._.._s_me__very__evil___file_name_");
    }

    @ParameterizedTest
    @ValueSource(strings = {"done", "rejected"})
    void cleanupOldCopies(String subdirName) throws IOException {
        var tempdir = Files.createTempDirectory("rsapi");
        var keepFileCopiesInDays = 90;
        var storage = new PhotoFileStorage(new WorkDir(tempdir.toString(), keepFileCopiesInDays));
        var subdir = tempdir.resolve("inbox").resolve(subdirName);
        var newFile = createFileWithLastModifiedInPast(subdir, "newFile.txt", keepFileCopiesInDays - 1);
        var oldFile = createFileWithLastModifiedInPast(subdir, "oldFile.txt", keepFileCopiesInDays + 1);

        storage.cleanupOldCopies();

        assertThat(Files.exists(newFile)).isEqualTo(true);
        assertThat(Files.exists(oldFile)).isEqualTo(false);
    }

    @NotNull
    private Path createFileWithLastModifiedInPast(Path subdir, String filename, int lastModifiedDaysInPast) throws IOException {
        var path = subdir.resolve(filename);
        Files.writeString(path, filename);
        Files.setLastModifiedTime(path, FileTime.from(Instant.now().minus(lastModifiedDaysInPast, ChronoUnit.DAYS)));
        return path;
    }

}