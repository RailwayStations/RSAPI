package org.railwaystations.rsapi.adapter.photostorage;

import org.apache.commons.io.IOUtils;
import org.railwaystations.rsapi.domain.model.Country;
import org.railwaystations.rsapi.domain.model.InboxEntry;
import org.railwaystations.rsapi.domain.model.Station;
import org.railwaystations.rsapi.domain.port.out.PhotoStorage;
import org.railwaystations.rsapi.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;

@Repository
public class PhotoFileStorage implements PhotoStorage {

    private static final Logger LOG = LoggerFactory.getLogger(PhotoFileStorage.class);

    private static final long MAX_SIZE = 20_000_000L;
    private static final String DONE_DIR = "done";
    private static final String REJECTED_DIR = "rejected";

    private final WorkDir workDir;

    public PhotoFileStorage(final WorkDir workDir) {
        this.workDir = workDir;
    }

    @Override
    public boolean isProcessed(final String filename) {
        return filename != null && new File(workDir.getInboxProcessedDir(), filename).exists();
    }

    @Override
    public void importPhoto(final InboxEntry inboxEntry, final Country country, final Station station) throws IOException {
        final File originalFile = getUploadFile(inboxEntry.getFilename());
        final File processedFile = new File(workDir.getInboxProcessedDir(), inboxEntry.getFilename());
        final File countryDir = new File(workDir.getPhotosDir(), country.getCode());
        FileUtils.deleteFile(countryDir, station.getKey().getId(), inboxEntry.getExtension());
        if (processedFile.exists()) {
            FileUtils.moveFile(processedFile, countryDir, station.getKey().getId(), inboxEntry.getExtension());
        } else {
            FileUtils.copyFile(originalFile, countryDir, station.getKey().getId(), inboxEntry.getExtension());
        }
        org.apache.commons.io.FileUtils.moveFileToDirectory(originalFile, new File(workDir.getInboxDir(), DONE_DIR), true);
    }

    @Override
    public void reject(final InboxEntry inboxEntry) throws IOException {
        final File file = getUploadFile(inboxEntry.getFilename());
        final File rejectDir = new File(workDir.getInboxDir(), REJECTED_DIR);
        org.apache.commons.io.FileUtils.moveFileToDirectory(file, rejectDir, true);
        org.apache.commons.io.FileUtils.deleteQuietly(new File(workDir.getInboxToProcessDir(), inboxEntry.getFilename()));
        org.apache.commons.io.FileUtils.deleteQuietly(new File(workDir.getInboxProcessedDir(), inboxEntry.getFilename()));
    }

    @Override
    public Long storeUpload(final InputStream body, final String filename) throws PhotoStorage.PhotoTooLargeException, IOException {
        final File file = getUploadFile(filename);
        LOG.info("Writing photo to {}", file);

        // write the file to the inbox directory
        org.apache.commons.io.FileUtils.forceMkdir(workDir.getInboxDir());
        final CheckedOutputStream cos = new CheckedOutputStream(new FileOutputStream(file), new CRC32());
        final long bytesRead = IOUtils.copyLarge(body, cos, 0L, MAX_SIZE);
        if (bytesRead == MAX_SIZE) {
            org.apache.commons.io.FileUtils.deleteQuietly(file);
            throw new PhotoStorage.PhotoTooLargeException(MAX_SIZE);
        }
        cos.close();

        // additionally write the file to the input directory for Vsion.AI
        org.apache.commons.io.FileUtils.forceMkdir(workDir.getInboxToProcessDir());
        Files.copy(file.toPath(), new File(workDir.getInboxToProcessDir(), file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        return cos.getChecksum().getValue();
    }

    @Override
    public File getUploadFile(final String filename) {
        return new File(workDir.getInboxDir(), filename);
    }

    @Override
    public File getPhotoFile(final String countryCode, final String filename) {
        return new File(new File(workDir.getPhotosDir(), FileUtils.sanitizeFilename(countryCode)), FileUtils.sanitizeFilename(filename));
    }

    @Override
    public File getInboxFile(final String filename) {
        return new File(workDir.getInboxDir(), FileUtils.sanitizeFilename(filename));
    }

    @Override
    public File getInboxProcessedFile(final String filename) {
        return new File(workDir.getInboxProcessedDir(), FileUtils.sanitizeFilename(filename));
    }

}
