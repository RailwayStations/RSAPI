package org.railwaystations.rsapi.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.railwaystations.rsapi.core.model.InboxEntry
import org.railwaystations.rsapi.core.model.StationTestFixtures.stationDe5WithPhoto
import org.railwaystations.rsapi.core.model.UserTestFixtures
import org.railwaystations.rsapi.core.ports.outbound.InboxPort
import org.railwaystations.rsapi.core.ports.outbound.MastodonPort
import org.railwaystations.rsapi.core.ports.outbound.PhotoPort
import org.railwaystations.rsapi.core.ports.outbound.StationPort
import org.railwaystations.rsapi.core.ports.outbound.UserPort


internal class SocialMediaServiceTest {
    private var stationPort = mockk<StationPort>()
    private var inboxPort = mockk<InboxPort>()
    private var photoPort = mockk<PhotoPort>()
    private var userPort = mockk<UserPort>()
    private var mastodonPort = mockk<MastodonPort>(relaxed = true)
    private var socialMediaService = SocialMediaService(
        inboxPort = inboxPort,
        photoPort = photoPort,
        stationPort = stationPort,
        userPort = userPort,
        mastodonPort = mastodonPort,
        stationUrl = "stationUrl"
    )

    @Test
    fun postNewPhotoToMastodon() {
        val inboxEntry = InboxEntry(
            countryCode = "de",
            stationId = "1234",
            photoId = 2L,
            title = "title",
            comment = "comment",
        )
        every { userPort.findById(0) } returns UserTestFixtures.someUser
        every { inboxPort.findOldestImportedPhotoNotYetPosted() } returns inboxEntry
        every { inboxPort.updatePosted(any()) } returns Unit

        socialMediaService.postRecentlyImportedPhotoNotYetPosted()

        verify {
            mastodonPort.postPhoto(
                """
                New railway station photo: title
                by someuser
                stationUrl?countryCode=de&stationId=1234&photoId=2
                comment
                #newrailwaystationphoto
                """.trimIndent()
            )
        }
    }

    @Test
    fun postDailyRandomPhoto() {
        val photoStation = stationDe5WithPhoto
        every { photoPort.countPhotos() } returns 42
        every { photoPort.findNthPhotoId(any()) } returns photoStation.photos.first().id
        every { stationPort.findByPhotoId(photoStation.photos.first().id) } returns photoStation

        socialMediaService.postDailyRandomPhoto()

        verify {
            mastodonPort.postPhoto(
                """
                Random daily railway station photo: Lummerland
                by Jim Knopf
                stationUrl?countryCode=de&stationId=5&photoId=0
                #randomdailyrailwaystationphoto
                """.trimIndent()
            )
        }
    }

}