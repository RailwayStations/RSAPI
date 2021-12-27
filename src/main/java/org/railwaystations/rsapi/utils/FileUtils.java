package org.railwaystations.rsapi.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class FileUtils {

    private static final Logger LOG = LoggerFactory.getLogger(FileUtils.class);

    public FileUtils() {
    }

    public static void deleteFile(final File dir, final String stationId, final String extension) {
        getCleanFile(dir, stationId, extension);
    }

    public static void moveFile(final File importFile, final File dir, final String stationId, final String extension) throws IOException {
        final File destFile = getCleanFile(dir, stationId, extension);
        Files.move(importFile.toPath(), destFile.toPath());
    }

    public static void copyFile(final File importFile, final File dir, final String stationId, final String extension) throws IOException {
        final File destFile = getCleanFile(dir, stationId, extension);
        Files.copy(importFile.toPath(), destFile.toPath());
    }

    private static File getCleanFile(final File dir, final String stationId, final String extension) {
        final File file = new File(dir, sanitizeFilename(stationId + "." + extension));
        try {
            Files.deleteIfExists(file.toPath());
        } catch (final IOException e) {
            LOG.warn("Couldn't delete file: " + file);
        }
        return file;
    }

    public static String sanitizeFilename(String fileName) {
        if (fileName == null) {
            return null;
        }

        fileName = fileName.replaceAll(" ", "_")
                .replaceAll("/", "_")
                .replaceAll(":", "_")
                .replaceAll("\"", "_")
                .replaceAll("\\|", "_")
                .replaceAll("\\*", "_")
                .replaceAll("\\?", "_")
                .replaceAll("<", "_")
                .replaceAll(">", "_");

        boolean done = false;
        while (!done) {
            final String replacedString = fileName.replace('\\', '_');
            done = (fileName.equals(replacedString));
            if (!done) {
                fileName = replacedString;
                break;
            }
        }
        return fileName;
    }

}
