package org.railwaystations.rsapi.adapter.task

import org.railwaystations.rsapi.core.ports.inbound.SocialMediaUseCase
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class MastodonBotTask(private val socialMediaUseCase: SocialMediaUseCase) {

    @Scheduled(fixedRate = 60000 * 60) // every hour
    fun postNewPhoto() {
        socialMediaUseCase.postRecentlyImportedPhotoNotYetPosted()
    }

    @Scheduled(fixedRate = 60000 * 60 * 24) // every day
    fun postDailyPhoto() {
        socialMediaUseCase.postDailyRandomPhoto()
    }

}
