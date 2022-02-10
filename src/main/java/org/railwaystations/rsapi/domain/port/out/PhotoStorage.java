package org.railwaystations.rsapi.domain.port.out;

import org.railwaystations.rsapi.domain.model.Country;
import org.railwaystations.rsapi.domain.model.InboxEntry;
import org.railwaystations.rsapi.domain.model.Station;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public interface PhotoStorage {

    boolean isProcessed(String filename);

    void importPhoto(InboxEntry inboxEntry, Country country, Station station) throws IOException;

    void reject(InboxEntry inboxEntry) throws IOException;

    File getUploadFile(String filename);

    Long storeUpload(InputStream body, String filename) throws IOException, PhotoTooLargeException;

    File getPhotoFile(String countryCode, String filename);

    File getInboxFile(String filename);

    File getInboxProcessedFile(String filename);

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
