package org.railwaystations.rsapi.core.model

import java.time.Instant

class InboxEntryTestFixtures {
    companion object {
        fun createInboxEntry(
            user: User,
            id: Int,
            countryCode: String,
            stationId: String,
            rejectReason: String?,
            done: Boolean
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
                createdAt = Instant.now(),
                done = done,
            )
        }
    }
}