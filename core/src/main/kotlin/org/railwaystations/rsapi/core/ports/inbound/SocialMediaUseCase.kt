package org.railwaystations.rsapi.core.ports.inbound

interface SocialMediaUseCase {
    fun postRecentlyImportedPhotoNotYetPosted()
    fun postDailyRandomPhoto()
}