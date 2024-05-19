package org.railwaystations.rsapi.adapter.task

import org.railwaystations.rsapi.core.ports.outbound.PhotoStoragePort
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class CleanupPhotoStorageTask(private val photoStoragePort: PhotoStoragePort) {

    @Scheduled(cron = "0 0 2 * * *")
    fun notifyUsers() {
        photoStoragePort.cleanupOldCopies()
    }
}
