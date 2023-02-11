package org.railwaystations.rsapi.adapter.in.task;

import lombok.RequiredArgsConstructor;
import org.railwaystations.rsapi.core.ports.out.PhotoStorage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CleanupPhotoStorageTask {

    private final PhotoStorage photoStorage;

    @Scheduled(cron = "0 0 2 * * *")
    public void notifyUsers() {
        photoStorage.cleanupOldCopies();
    }

}
