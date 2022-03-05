package org.railwaystations.rsapi.core.ports;

import org.railwaystations.rsapi.core.model.Country;
import org.railwaystations.rsapi.core.model.InboxEntry;
import org.railwaystations.rsapi.core.model.Station;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface PhotoStorage {

    boolean isProcessed(String filename);

    void importPhoto(InboxEntry inboxEntry, Country country, Station station) throws IOException;

    void reject(InboxEntry inboxEntry) throws IOException;

    Path getUploadFile(String filename);

    Long storeUpload(InputStream body, String filename) throws IOException, PhotoTooLargeException;

    Path getPhotoFile(String countryCode, String filename);

    Path getInboxFile(String filename);

    Path getInboxProcessedFile(String filename);

    void cleanupOldCopies();

    class PhotoTooLargeException extends Exception {
        private final long maxSize;

        public PhotoTooLargeException(final long maxSize) {
            this.maxSize = maxSize;
        }

        public long getMaxSize() {
            return maxSize;
        }
    }

}
