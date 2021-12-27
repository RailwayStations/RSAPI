package org.railwaystations.rsapi.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class FileUtilsTest {

    private File temp;

    @BeforeEach
    public void setUp() throws IOException {
        temp = Files.createTempDirectory(null).toFile();
    }

    @Test
    public void testMove() throws IOException {
        final File photo = new File(temp, "photo.jpg");
        Files.writeString(photo.toPath(), "test");

        final File destDir = new File(temp, "de");
        destDir.mkdir();

        FileUtils.moveFile(photo, destDir, "0815", "jpg");

        final Path destFile = new File(destDir, "0815.jpg").toPath();
        assertThat("Dest file exists", Files.exists(destFile));
        assertThat("Source file gone", Files.notExists(photo.toPath()));

        assertThat(Files.readString(destFile), equalTo("test"));
    }

    @Test
    public void testCopy() throws IOException {
        final File photo = new File(temp, "photo.jpg");
        Files.writeString(photo.toPath(), "test");

        final File destDir = new File(temp, "de");
        destDir.mkdir();

        FileUtils.copyFile(photo, destDir, "0815", "jpg");

        final Path destFile = new File(destDir, "0815.jpg").toPath();
        assertThat("Dest file exists", Files.exists(destFile));
        assertThat("Source file exists", Files.exists(photo.toPath()));

        assertThat(Files.readString(destFile), equalTo("test"));
    }

}
