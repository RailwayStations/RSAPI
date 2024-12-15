package org.railwaystations.rsapi.adapter.db

import org.jooq.DSLContext
import org.jooq.Record7
import org.jooq.SelectOnConditionStep
import org.jooq.impl.DSL.power
import org.jooq.impl.DSL.sqrt
import org.jooq.impl.DSL.value
import org.railwaystations.rsapi.adapter.db.jooq.tables.records.InboxRecord
import org.railwaystations.rsapi.adapter.db.jooq.tables.records.StationRecord
import org.railwaystations.rsapi.adapter.db.jooq.tables.references.InboxTable
import org.railwaystations.rsapi.adapter.db.jooq.tables.references.PhotoTable
import org.railwaystations.rsapi.adapter.db.jooq.tables.references.StationTable
import org.railwaystations.rsapi.adapter.db.jooq.tables.references.UserTable
import org.railwaystations.rsapi.core.model.Coordinates
import org.railwaystations.rsapi.core.model.InboxEntry
import org.railwaystations.rsapi.core.model.ProblemReportType
import org.railwaystations.rsapi.core.model.PublicInboxEntry
import org.railwaystations.rsapi.core.ports.outbound.InboxPort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

@Component
class InboxAdapter(private val dsl: DSLContext) : InboxPort {

    override fun findById(id: Long): InboxEntry? {
        return selectInboxEntries()
            .where(InboxTable.id.eq(id))
            .fetchOne()?.toInboxEntry()
    }

    private fun selectInboxEntries(): SelectOnConditionStep<Record7<InboxRecord, String?, Double?, Double?, String?, String?, String?>> =
        dsl.select(
            InboxTable,
            StationTable.title,
            StationTable.lat,
            StationTable.lon,
            UserTable.name,
            UserTable.email,
            PhotoTable.urlpath
        )
            .from(InboxTable)
            .join(UserTable).on(UserTable.id.eq(InboxTable.photographerid))
            .leftJoin(StationTable)
            .on(StationTable.countrycode.eq(InboxTable.countrycode).and(StationTable.id.eq(InboxTable.stationid)))
            .leftJoin(PhotoTable).on(
                PhotoTable.countrycode.eq(InboxTable.countrycode)
                    .and(
                        PhotoTable.stationid.eq(InboxTable.stationid)
                            .and(
                                PhotoTable.id.eq(InboxTable.photoid)
                                    .or(PhotoTable.primary.eq(true).and(InboxTable.photoid.isNull))
                            )
                    )
            )

    private fun Record7<InboxRecord, String?, Double?, Double?, String?, String?, String?>.toInboxEntry() =
        InboxEntry(
            id = value1().id!!,
            countryCode = value1().countrycode,
            stationId = value1().stationid,
            photoId = value1().photoid,
            title = value2(),
            newTitle = value1().title,
            coordinates = createCoordinates(value3(), value4()),
            newCoordinates = createCoordinates(value1().lat, value1().lon),
            photographerId = value1().photographerid,
            photographerNickname = value5(),
            photographerEmail = value6(),
            extension = value1().extension,
            comment = value1().comment,
            rejectReason = value1().rejectreason,
            createdAt = value1().createdat,
            done = value1().done,
            existingPhotoUrlPath = value7(),
            crc32 = value1().crc32,
            problemReportType = value1().problemreporttype?.let { ProblemReportType.valueOf(it) },
            active = value1().active,
            notified = value1().notified,
            posted = value1().posted
        )

    private fun createCoordinates(lat: Double?, lon: Double?): Coordinates? {
        if (lat == null || lon == null) return null

        return Coordinates(lat, lon)
    }

    override fun findPendingInboxEntries() =
        selectInboxEntries()
            .where(InboxTable.done.eq(false))
            .orderBy<Long?>(InboxTable.id)
            .fetch().map { it.toInboxEntry() }

    override fun findOldestImportedPhotoNotYetPosted() =
        selectInboxEntries()
            .where(
                InboxTable.done.eq(true)
                    .and(InboxTable.rejectreason.isNull)
                    .and(InboxTable.extension.isNotNull)
                    .and(InboxTable.posted.eq(false))
            )
            .orderBy<Instant?>(InboxTable.createdat)
            .limit(1)
            .fetchOne()?.toInboxEntry()

    override fun findPublicInboxEntries(): List<PublicInboxEntry> {
        val result = dsl.select(InboxTable, StationTable)
            .from(InboxTable)
            .leftJoin(StationTable)
            .on(StationTable.countrycode.eq(InboxTable.countrycode).and(StationTable.id.eq(InboxTable.stationid)))
            .where(InboxTable.done.eq(false).and(InboxTable.problemreporttype.isNull))
            .fetch()

        return result.map { it.value1().toPublicInboxEntry(it.value2()) }
    }

    private fun InboxRecord.toPublicInboxEntry(stationRecord: StationRecord?) =
        PublicInboxEntry(
            countryCode = countrycode,
            stationId = stationid,
            title = stationRecord?.title ?: title!!,
            coordinates = stationRecord?.let { Coordinates(it.lat, it.lon) } ?: Coordinates(lat ?: 0.0, lon ?: 0.0)
        )

    @Transactional
    override fun insert(inboxEntry: InboxEntry): Long {
        val inboxRecord = InboxRecord(
            id = null,
            photographerid = inboxEntry.photographerId,
            countrycode = inboxEntry.countryCode,
            stationid = inboxEntry.stationId,
            title = inboxEntry.title,
            lat = inboxEntry.lat,
            lon = inboxEntry.lon,
            extension = inboxEntry.extension,
            comment = inboxEntry.comment,
            rejectreason = inboxEntry.rejectReason,
            done = inboxEntry.done,
            problemreporttype = inboxEntry.problemReportType?.name,
            active = inboxEntry.active,
            crc32 = inboxEntry.crc32,
            notified = inboxEntry.notified,
            createdat = inboxEntry.createdAt,
            photoid = inboxEntry.photoId,
            posted = inboxEntry.posted,
        )
        dsl.attach(inboxRecord)
        inboxRecord.store()
        return inboxRecord.id!!
    }

    @Transactional
    override fun reject(id: Long, rejectReason: String) {
        dsl.update(InboxTable)
            .set(InboxTable.rejectreason, rejectReason)
            .set(InboxTable.done, true)
            .where(InboxTable.id.eq(id))
            .execute()
    }

    @Transactional
    override fun done(id: Long) {
        dsl.update(InboxTable)
            .set(InboxTable.done, true)
            .where(InboxTable.id.eq(id))
            .execute()
    }

    override fun countPendingInboxEntriesForStation(id: Long?, countryCode: String, stationId: String): Int =
        dsl.selectCount()
            .from(InboxTable)
            .where(
                InboxTable.done.eq(false)
                    .and(InboxTable.countrycode.eq(countryCode))
                    .and(InboxTable.stationid.eq(stationId))
                    .and(id?.let { InboxTable.id.ne(it) })
            )
            .fetchOne(0, Int::class.java) ?: 0

    override fun countPendingInboxEntries(): Int =
        dsl.selectCount()
            .from(InboxTable)
            .where(InboxTable.done.eq(false))
            .fetchOne(0, Int::class.java) ?: 0

    /**
     * Count nearby pending uploads using simple pythagoras (only valid for a few km)
     */
    override fun countPendingInboxEntriesForNearbyCoordinates(id: Long?, coordinates: Coordinates): Int =
        dsl.selectCount()
            .from(InboxTable)
            .where(
                InboxTable.done.eq(false)
                    .and(InboxTable.id.ne(id).or(value(id).isNull))
                    .and(
                        sqrt(
                            power(value(71.5).mul(InboxTable.lon.minus(coordinates.lon)), 2)
                                .plus(power(value(111.3).mul(InboxTable.lat.minus(coordinates.lat)), 2))
                        ).lt(BigDecimal(0.5))
                    )
            )
            .fetchOne(0, Int::class.java) ?: 0

    @Transactional
    override fun updateCrc32(id: Long, crc32: Long) {
        dsl.update(InboxTable)
            .set(InboxTable.crc32, crc32)
            .where(InboxTable.id.eq(id))
            .execute()
    }

    override fun findInboxEntriesToNotify() =
        selectInboxEntries()
            .where(InboxTable.done.eq(true).and(InboxTable.notified.eq(false)))
            .orderBy<Long?>(InboxTable.id)
            .fetch().map { it.toInboxEntry() }

    @Transactional
    override fun updateNotified(ids: List<Long>) {
        dsl.update(InboxTable)
            .set(InboxTable.notified, true)
            .where(InboxTable.id.`in`(ids))
            .execute()
    }

    @Transactional
    override fun updatePosted(id: Long) {
        dsl.update(InboxTable)
            .set(InboxTable.posted, true)
            .where(InboxTable.id.eq(id))
            .execute()
    }

    @Transactional
    override fun updatePhotoId(id: Long, photoId: Long) {
        dsl.update(InboxTable)
            .set(InboxTable.photoid, photoId)
            .where(InboxTable.id.eq(id))
            .execute()
    }

    @Transactional
    override fun updateMissingStationImported(id: Long, countryCode: String, stationId: String, title: String) {
        dsl.update(InboxTable)
            .set(InboxTable.done, true)
            .set(InboxTable.countrycode, countryCode)
            .set(InboxTable.stationid, stationId)
            .set(InboxTable.title, title)
            .where(InboxTable.id.eq(id))
            .execute()
    }

    override fun findByUser(photographerId: Long, includeCompletedEntries: Boolean) =
        selectInboxEntries()
            .where(
                InboxTable.photographerid.eq(photographerId)
                    .and(InboxTable.done.eq(false).or(value(includeCompletedEntries).eq(true)))
            )
            .orderBy(InboxTable.id.desc())
            .fetch().map { it.toInboxEntry() }

    override fun findPendingByStation(countryCode: String, stationId: String) =
        selectInboxEntries()
            .where(
                InboxTable.countrycode.eq(countryCode)
                    .and(InboxTable.stationid.eq(stationId))
                    .and(InboxTable.done.eq(false))
            )
            .fetch().map { it.toInboxEntry() }

}
