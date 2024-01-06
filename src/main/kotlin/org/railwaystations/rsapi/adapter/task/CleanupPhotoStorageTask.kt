package org.railwaystations.rsapi.adapter.task

import org.railwaystations.rsapi.core.ports.PhotoStorage
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class CleanupPhotoStorageTask(private val photoStorage: PhotoStorage) {

    @Scheduled(cron = "0 0 2 * * *")
    fun notifyUsers() {
        photoStorage.cleanupOldCopies()
    }
}
