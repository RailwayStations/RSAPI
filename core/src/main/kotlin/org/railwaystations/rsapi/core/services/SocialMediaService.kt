package org.railwaystations.rsapi.core.services

import org.railwaystations.rsapi.core.model.*
import org.railwaystations.rsapi.core.ports.inbound.SocialMediaUseCase
import org.railwaystations.rsapi.core.ports.outbound.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Random

@Service
class SocialMediaService(
    private val inboxPort: InboxPort,
    private val photoPort: PhotoPort,
    private val stationPort: StationPort,
    private val userPort: UserPort,
    private val mastodonPort: MastodonPort,
    @param:Value(
        "\${mastodon-bot.stationUrl}"
    ) private val stationUrl: String
) : SocialMediaUseCase {

    override fun postRecentlyImportedPhotoNotYetPosted() {
        val inboxEntry = inboxPort.findOldestImportedPhotoNotYetPosted()
        if (inboxEntry == null) {
            return
        }
        val photographer = userPort.findById(inboxEntry.photographerId)
        var status =
            """
            New railway station photo: ${inboxEntry.title}
            by ${photographer?.displayName ?: ANONYM}
            $stationUrl?countryCode=${inboxEntry.countryCode}&stationId=${inboxEntry.stationId}&photoId=${inboxEntry.photoId}
            """.trimIndent()
        if (!inboxEntry.comment.isNullOrBlank()) {
            status += "\n${inboxEntry.comment}"
        }
        status += "\n#newrailwaystationphoto"
        mastodonPort.postPhoto(status)
        inboxPort.updatePosted(inboxEntry.id)
    }

    override fun postDailyRandomPhoto() {
        val photoCount = photoPort.countPhotos()
        if (photoCount == 0L) {
            return
        }
        val rand = Random()
        val photoId = photoPort.findNthPhotoId(rand.nextLong(photoCount))
        val station = stationPort.findByPhotoId(photoId)
        val photo = station.photos.first()
        val status =
            """
            Random daily railway station photo: ${station.title}
            by ${photo.photographer.displayName}
            $stationUrl?countryCode=${station.key.country}&stationId=${station.key.id}&photoId=${photo.id}
            #randomdailyrailwaystationphoto
            """.trimIndent()
        mastodonPort.postPhoto(status)
    }

}
