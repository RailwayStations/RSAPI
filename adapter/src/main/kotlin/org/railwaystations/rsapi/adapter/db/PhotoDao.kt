package org.railwaystations.rsapi.adapter.db

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.railwaystations.rsapi.core.model.Photo
import org.railwaystations.rsapi.core.model.Station
import org.railwaystations.rsapi.core.ports.outbound.PhotoPort

interface PhotoDao : PhotoPort {
    @SqlUpdate("INSERT INTO photos (countryCode, stationId, `primary`, urlPath, license, photographerId, createdAt) VALUES (:stationKey.country, :stationKey.id, :primary, :urlPath, :license, :photographer.id, :createdAt)")
    @GetGeneratedKeys("id")
    override fun insert(@BindBean photo: Photo?): Long

    @SqlUpdate("UPDATE photos SET `primary` = :primary, urlPath = :urlPath, license = :license, photographerId = :photographer.id, createdAt = :createdAt, outdated = :outdated WHERE id = :id")
    override fun update(@BindBean photo: Photo?)

    @SqlUpdate("DELETE FROM photos WHERE id = :id")
    override fun delete(@Bind("id") id: Long)

    @SqlUpdate("UPDATE photos SET outdated = true WHERE id = :id")
    override fun updatePhotoOutdated(@Bind("id") id: Long)

    @SqlUpdate("UPDATE photos SET `primary` = false WHERE countryCode = :country and stationId = :id")
    override fun setAllPhotosForStationSecondary(@BindBean key: Station.Key?)

    @SqlUpdate("UPDATE photos SET `primary` = true WHERE id = :id")
    override fun setPrimary(@Bind("id") id: Long)

    @SqlQuery("SELECT count(*) FROM photos")
    override fun countPhotos(): Long

    @SqlQuery("SELECT id FROM photos LIMIT 1 OFFSET :n")
    override fun findNthPhotoId(@Bind("n") n: Long): Long
}
