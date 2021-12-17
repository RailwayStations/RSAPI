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

    public static void moveFile(final File importFile, final File countryDir, final String stationId, final String extension) throws IOException {
        final File destFile = getCleanDestFile(countryDir, stationId, extension);
        Files.move(importFile.toPath(), destFile.toPath());
    }

    public static void copyFile(final File importFile, final File countryDir, final String stationId, final String extension) throws IOException {
        final File destFile = getCleanDestFile(countryDir, stationId, extension);
        Files.copy(importFile.toPath(), destFile.toPath());
    }

    private static File getCleanDestFile(final File countryDir, final String stationId, final String extension) {
        final File destFile = new File(countryDir, stationId + "." + extension);
        try {
            Files.deleteIfExists(destFile.toPath());
        } catch (final IOException e) {
            LOG.warn("Couldn't delete file: " + destFile);
        }
        return destFile;
    }

    public static String sanitizeFileName(String fileName) {
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
