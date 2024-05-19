package org.railwaystations.rsapi.adapter.task

import org.railwaystations.rsapi.core.ports.PostRecentlyImportedPhotoUseCase
import org.railwaystations.rsapi.core.utils.Logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class MastodonBotTask(private val postRecentlyImportedPhotoUseCase: PostRecentlyImportedPhotoUseCase) {

    private val log by Logger()

    @Scheduled(fixedRate = 60000 * 60) // every hour
    fun postNewPhoto() {
        log.info("Starting MastodonBotTask")
        postRecentlyImportedPhotoUseCase.postRecentlyImportedPhotoNotYetPosted()
    }
}
