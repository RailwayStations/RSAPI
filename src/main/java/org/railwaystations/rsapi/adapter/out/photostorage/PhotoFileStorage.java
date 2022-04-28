package org.railwaystations.rsapi.adapter.out.photostorage;

import org.apache.commons.io.IOUtils;
import org.railwaystations.rsapi.core.model.Country;
import org.railwaystations.rsapi.core.model.InboxEntry;
import org.railwaystations.rsapi.core.model.Station;
import org.railwaystations.rsapi.core.ports.out.PhotoStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Repository
public class PhotoFileStorage implements PhotoStorage {

    private static final Logger LOG = LoggerFactory.getLogger(PhotoFileStorage.class);

    private static final long MAX_SIZE = 20_000_000L;

    private final WorkDir workDir;

    public PhotoFileStorage(final WorkDir workDir) {
        this.workDir = workDir;
    }

    @Override
    public boolean isProcessed(final String filename) {
        return filename != null && Files.exists(workDir.getInboxProcessedDir().resolve(filename));
    }

    @Override
    public void importPhoto(final InboxEntry inboxEntry, final Country country, final Station station) throws IOException {
        final var uploadedFile = getUploadFile(inboxEntry.getFilename());
        final var processedFile = workDir.getInboxProcessedDir().resolve(inboxEntry.getFilename());
        final var destinationFile = workDir.getPhotosDir().resolve(country.getCode()).resolve(sanitizeFilename(station.getKey().id() + "." + inboxEntry.getExtension()));
        if (Files.exists(processedFile)) {
            Files.move(processedFile, destinationFile, REPLACE_EXISTING);
        } else {
            Files.copy(uploadedFile, destinationFile, REPLACE_EXISTING);
        }
        try {
            Files.move(uploadedFile, workDir.getInboxDoneDir().resolve(uploadedFile.getFileName()), REPLACE_EXISTING);
        } catch (final Exception e) {
            LOG.warn("Couldn't move original file {} to done dir", uploadedFile, e);
        }
    }

    @Override
    public void reject(final InboxEntry inboxEntry) throws IOException {
        final var file = getUploadFile(inboxEntry.getFilename());
        Files.move(file, workDir.getInboxRejectedDir().resolve(file.getFileName()), REPLACE_EXISTING);
        Files.deleteIfExists(workDir.getInboxToProcessDir().resolve(inboxEntry.getFilename()));
        Files.deleteIfExists(workDir.getInboxProcessedDir().resolve(inboxEntry.getFilename()));
    }

    @Override
    public Long storeUpload(final InputStream body, final String filename) throws PhotoStorage.PhotoTooLargeException, IOException {
        final var file = getUploadFile(filename);
        LOG.info("Writing photo to {}", file);

        // write the file to the inbox directory
        final var cos = new CheckedOutputStream(Files.newOutputStream(file), new CRC32());
        final var bytesRead = IOUtils.copyLarge(body, cos, 0L, MAX_SIZE);
        if (bytesRead == MAX_SIZE) {
            Files.deleteIfExists(file);
            throw new PhotoStorage.PhotoTooLargeException(MAX_SIZE);
        }
        cos.close();

        // additionally write the file to the input directory for Vsion.AI
        Files.copy(file, workDir.getInboxToProcessDir().resolve(file.getFileName()), REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        return cos.getChecksum().getValue();
    }

    @Override
    public Path getUploadFile(final String filename) {
        return workDir.getInboxDir().resolve(filename);
    }

    @Override
    public Path getPhotoFile(final String countryCode, final String filename) {
        return workDir.getPhotosDir().resolve(sanitizeFilename(countryCode)).resolve(sanitizeFilename(filename));
    }

    @Override
    public Path getInboxFile(final String filename) {
        return workDir.getInboxDir().resolve(sanitizeFilename(filename));
    }

    @Override
    public Path getInboxProcessedFile(final String filename) {
        return workDir.getInboxProcessedDir().resolve(sanitizeFilename(filename));
    }

    @Override
    public void cleanupOldCopies() {
        final var maxAge = Instant.now().minus(workDir.getKeepFileCopiesInDays(), ChronoUnit.DAYS);
        cleanupOldCopiesFrom(workDir.getInboxDoneDir(), maxAge);
        cleanupOldCopiesFrom(workDir.getInboxRejectedDir(), maxAge);
    }

    static void cleanupOldCopiesFrom(final Path dir, final Instant maxAge) {
        try {
            Files.list(dir)
                    .filter(Files::isRegularFile)
                    .filter(f -> isOlderThan(f, maxAge))
                    .forEach(PhotoFileStorage::deleteSilently);
        } catch (final Exception e) {
            LOG.error("Failed to cleanup old copies from {}", dir, e);
        }
    }

    static void deleteSilently(final Path path) {
        try {
            Files.delete(path);
            LOG.info("Deleted {}", path);
        } catch (final IOException e) {
            LOG.warn("Unable to delete {}", path);
        }
    }

    static boolean isOlderThan(final Path path, final Instant maxAge) {
        try {
            return Files.getLastModifiedTime(path).toInstant().isBefore(maxAge);
        } catch (final IOException e) {
            LOG.warn("Unable to getLastModifiedTime of {}", path.getFileName());
        }
        return false;
    }

    static String sanitizeFilename(final String fileName) {
        if (fileName == null) {
            return null;
        }

        return fileName.replace(" ", "_")
                .replace("/", "_")
                .replace(":", "_")
                .replace("\"", "_")
                .replace("|", "_")
                .replace("*", "_")
                .replace("?", "_")
                .replace("<", "_")
                .replace(">", "_")
                .replace('\\', '_');
    }

}
