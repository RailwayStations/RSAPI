package org.railwaystations.rsapi.core.ports

import org.railwaystations.rsapi.core.model.Coordinates
import org.railwaystations.rsapi.core.model.InboxEntry
import org.railwaystations.rsapi.core.model.PublicInboxEntry

interface InboxPort {
    fun findById(id: Long): InboxEntry?
    fun findPendingInboxEntries(): List<InboxEntry>
    fun findRecentlyImportedPhotosNotYetPosted(): List<InboxEntry>
    fun findPublicInboxEntries(): List<PublicInboxEntry>
    fun insert(inboxEntry: InboxEntry): Long
    fun reject(id: Long, rejectReason: String)
    fun done(id: Long)
    fun countPendingInboxEntriesForStation(id: Long?, countryCode: String, stationId: String): Int
    fun countPendingInboxEntries(): Long
    fun countPendingInboxEntriesForNearbyCoordinates(id: Long?, coordinates: Coordinates): Int
    fun updateCrc32(id: Long, crc32: Long)
    fun findInboxEntriesToNotify(): List<InboxEntry>
    fun updateNotified(ids: List<Long>)
    fun updatePosted(id: Long)
    fun updatePhotoId(id: Long, photoId: Long)
    fun updateMissingStationImported(id: Long, countryCode: String, stationId: String, title: String)
    fun findByUser(photographerId: Int, showCompletedEntries: Boolean): List<InboxEntry>
    fun findPendingByStation(countryCode: String, stationId: String): List<InboxEntry>
}
