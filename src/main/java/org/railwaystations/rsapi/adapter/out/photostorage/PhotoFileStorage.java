package org.railwaystations.rsapi.adapter.out.photostorage;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.railwaystations.rsapi.core.model.Country;
import org.railwaystations.rsapi.core.model.InboxEntry;
import org.railwaystations.rsapi.core.model.Station;
import org.railwaystations.rsapi.core.ports.out.PhotoStorage;
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
@Slf4j
public class PhotoFileStorage implements PhotoStorage {

    private static final long MAX_SIZE = 20_000_000L;

    private final WorkDir workDir;

    public PhotoFileStorage(WorkDir workDir) {
        this.workDir = workDir;
    }

    @Override
    public boolean isProcessed(String filename) {
        return filename != null && Files.exists(workDir.getInboxProcessedDir().resolve(filename));
    }

    @Override
    public void importPhoto(InboxEntry inboxEntry, Country country, Station station) throws IOException {
        var uploadedFile = getUploadFile(inboxEntry.getFilename());
        var processedFile = workDir.getInboxProcessedDir().resolve(inboxEntry.getFilename());
        // TODO: add photoId to the destinationFile
        var destinationFile = workDir.getPhotosDir().resolve(country.getCode()).resolve(sanitizeFilename(station.getKey().getId() + "." + inboxEntry.getExtension()));
        Files.createDirectories(workDir.getPhotosDir().resolve(country.getCode()));
        if (Files.exists(processedFile)) {
            Files.move(processedFile, destinationFile, REPLACE_EXISTING);
        } else {
            Files.copy(uploadedFile, destinationFile, REPLACE_EXISTING);
        }
        try {
            Files.move(uploadedFile, workDir.getInboxDoneDir().resolve(uploadedFile.getFileName()), REPLACE_EXISTING);
        } catch (Exception e) {
            log.warn("Couldn't move original file {} to done dir", uploadedFile, e);
        }
    }

    @Override
    public void reject(InboxEntry inboxEntry) throws IOException {
        var file = getUploadFile(inboxEntry.getFilename());
        Files.move(file, workDir.getInboxRejectedDir().resolve(file.getFileName()), REPLACE_EXISTING);
        Files.deleteIfExists(workDir.getInboxToProcessDir().resolve(inboxEntry.getFilename()));
        Files.deleteIfExists(workDir.getInboxProcessedDir().resolve(inboxEntry.getFilename()));
    }

    @Override
    public Long storeUpload(InputStream body, String filename) throws PhotoStorage.PhotoTooLargeException, IOException {
        var file = getUploadFile(filename);
        log.info("Writing photo to {}", file);

        // write the file to the inbox directory
        var cos = new CheckedOutputStream(Files.newOutputStream(file), new CRC32());
        var bytesRead = IOUtils.copyLarge(body, cos, 0L, MAX_SIZE);
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
    public Path getUploadFile(String filename) {
        return workDir.getInboxDir().resolve(filename);
    }

    @Override
    public Path getPhotoFile(String countryCode, String filename) {
        return workDir.getPhotosDir().resolve(sanitizeFilename(countryCode)).resolve(sanitizeFilename(filename));
    }

    @Override
    public Path getInboxFile(String filename) {
        return workDir.getInboxDir().resolve(sanitizeFilename(filename));
    }

    @Override
    public Path getInboxProcessedFile(String filename) {
        return workDir.getInboxProcessedDir().resolve(sanitizeFilename(filename));
    }

    @Override
    public Path getInboxToProcessFile(String filename) {
        return workDir.getInboxToProcessDir().resolve(sanitizeFilename(filename));
    }

    @Override
    public void cleanupOldCopies() {
        var maxAge = Instant.now().minus(workDir.getKeepFileCopiesInDays(), ChronoUnit.DAYS);
        cleanupOldCopiesFrom(workDir.getInboxDoneDir(), maxAge);
        cleanupOldCopiesFrom(workDir.getInboxRejectedDir(), maxAge);
    }

    static void cleanupOldCopiesFrom(Path dir, Instant maxAge) {
        try (var pathStream = Files.list(dir)){
            pathStream
                .filter(Files::isRegularFile)
                .filter(f -> isOlderThan(f, maxAge))
                .forEach(PhotoFileStorage::deleteSilently);
        } catch (Exception e) {
            log.error("Failed to cleanup old copies from {}", dir, e);
        }
    }

    static void deleteSilently(Path path) {
        try {
            Files.delete(path);
            log.info("Deleted {}", path);
        } catch (IOException e) {
            log.warn("Unable to delete {}", path);
        }
    }

    static boolean isOlderThan(Path path, Instant maxAge) {
        try {
            return Files.getLastModifiedTime(path).toInstant().isBefore(maxAge);
        } catch (IOException e) {
            log.warn("Unable to getLastModifiedTime of {}", path.getFileName());
        }
        return false;
    }

    static String sanitizeFilename(String fileName) {
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
