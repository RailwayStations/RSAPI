package org.railwaystations.rsapi.adapter.in.task;

import org.railwaystations.rsapi.core.ports.out.PhotoStorage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CleanupPhotoStorageTask {

    private final PhotoStorage photoStorage;

    public CleanupPhotoStorageTask(PhotoStorage photoStorage) {
        super();
        this.photoStorage = photoStorage;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void notifyUsers() {
        photoStorage.cleanupOldCopies();
    }

}
