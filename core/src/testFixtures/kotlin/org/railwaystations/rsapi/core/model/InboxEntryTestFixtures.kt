package org.railwaystations.rsapi.core.model

import org.railwaystations.rsapi.core.model.StationTestFixtures.stationDe8000
import org.railwaystations.rsapi.core.model.UserTestFixtures.user10
import java.time.Instant.now
import java.time.temporal.ChronoUnit

object InboxEntryTestFixtures {

    val photoUploadDe8000 = InboxEntry(
        id = 0L,
        countryCode = stationDe8000.key.country,
        stationId = stationDe8000.key.id,
        photographerId = user10.id,
        extension = "jpg",
        crc32 = System.currentTimeMillis(),
        createdAt = now().truncatedTo(ChronoUnit.SECONDS),
        comment = "Some important comment.",
    )

    val problemReportDe8000 = InboxEntry(
        id = 0L,
        countryCode = stationDe8000.key.country,
        stationId = stationDe8000.key.id,
        photographerId = user10.id,
        existingPhotoUrlPath = "de/4711.jpg",
        photoId = 4711,
        createdAt = now(),
        comment = "Photo outdated",
        problemReportType = ProblemReportType.PHOTO_OUTDATED,
    )

    val photoUploadMissingStation = InboxEntry(
        id = 0L,
        title = "Missing Station",
        coordinates = Coordinates(50.1, 9.7),
        photographerId = user10.id,
        extension = "jpg",
        crc32 = System.currentTimeMillis(),
        createdAt = now().truncatedTo(ChronoUnit.SECONDS),
        comment = "Some missing station.",
    )

    fun createInboxEntry(
        user: User,
        id: Int = 0,
        countryCode: String,
        stationId: String,
        rejectReason: String? = null,
        done: Boolean = false
    ): InboxEntry {
        return InboxEntry(
            id = id.toLong(),
            countryCode = countryCode,
            stationId = stationId,
            title = "Station $stationId",
            coordinates = Coordinates(50.1, 9.2),
            photographerId = user.id,
            photographerNickname = user.name,
            extension = "jpg",
            rejectReason = rejectReason,
            createdAt = now(),
            done = done,
        )
    }
}