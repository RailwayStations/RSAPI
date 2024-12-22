package org.railwaystations.rsapi.adapter.db

import org.jooq.DSLContext
import org.railwaystations.rsapi.adapter.db.jooq.tables.records.PhotoRecord
import org.railwaystations.rsapi.adapter.db.jooq.tables.references.PhotoTable
import org.railwaystations.rsapi.core.model.Photo
import org.railwaystations.rsapi.core.model.Station
import org.railwaystations.rsapi.core.ports.outbound.PhotoPort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PhotoAdapter(private val dsl: DSLContext) : PhotoPort {

    @Transactional
    override fun insert(photo: Photo): Long {
        val photosRecord = PhotoRecord(
            id = null,
            countrycode = photo.stationKey.country,
            stationid = photo.stationKey.id,
            primary = photo.primary,
            outdated = photo.outdated,
            urlpath = photo.urlPath,
            license = photo.license.name,
            photographerid = photo.photographer.id,
            createdat = photo.createdAt,
        )
        dsl.attach(photosRecord)
        photosRecord.store()
        return photosRecord.id!!
    }

    @Transactional
    override fun update(photo: Photo) {
        dsl.update(PhotoTable)
            .set(PhotoTable.primary, photo.primary)
            .set(PhotoTable.urlpath, photo.urlPath)
            .set(PhotoTable.license, photo.license.name)
            .set(PhotoTable.photographerid, photo.photographer.id)
            .set(PhotoTable.createdat, photo.createdAt)
            .set(PhotoTable.outdated, photo.outdated)
            .where(PhotoTable.id.eq(photo.id))
            .execute()
    }

    @Transactional
    override fun delete(id: Long) {
        dsl.deleteFrom(PhotoTable)
            .where(PhotoTable.id.eq(id))
            .execute()
    }

    @Transactional
    override fun updatePhotoOutdated(id: Long) {
        dsl.update(PhotoTable)
            .set(PhotoTable.outdated, true)
            .where(PhotoTable.id.eq(id))
            .execute()
    }

    @Transactional
    override fun setAllPhotosForStationSecondary(key: Station.Key) {
        dsl.update(PhotoTable)
            .set(PhotoTable.primary, false)
            .where(PhotoTable.countrycode.eq(key.country).and(PhotoTable.stationid.eq(key.id)))
            .execute()

    }

    @Transactional
    override fun setPrimary(id: Long) {
        dsl.update(PhotoTable)
            .set(PhotoTable.primary, true)
            .where(PhotoTable.id.eq(id))
            .execute()
    }

    override fun countPhotos(): Long {
        return dsl.fetchCount(PhotoTable).toLong()
    }

    override fun findNthPhotoId(n: Long): Long {
        return dsl.select(PhotoTable.id)
            .from(PhotoTable)
            .offset(n)
            .limit(1)
            .fetchOne(PhotoTable.id)!!
    }

}
