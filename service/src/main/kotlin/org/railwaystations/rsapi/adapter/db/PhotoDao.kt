package org.railwaystations.rsapi.adapter.db

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.railwaystations.rsapi.core.model.Photo
import org.railwaystations.rsapi.core.model.Station

interface PhotoDao {
    @SqlUpdate("INSERT INTO photos (countryCode, stationId, `primary`, urlPath, license, photographerId, createdAt) VALUES (:stationKey.country, :stationKey.id, :primary, :urlPath, :license, :photographer.id, :createdAt)")
    @GetGeneratedKeys("id")
    fun insert(@BindBean photo: Photo?): Long

    @SqlUpdate("UPDATE photos SET `primary` = :primary, urlPath = :urlPath, license = :license, photographerId = :photographer.id, createdAt = :createdAt, outdated = :outdated WHERE id = :id")
    fun update(@BindBean photo: Photo?)

    @SqlUpdate("DELETE FROM photos WHERE id = :id")
    fun delete(@Bind("id") id: Long)

    @SqlUpdate("UPDATE photos SET outdated = true WHERE id = :id")
    fun updatePhotoOutdated(@Bind("id") id: Long)

    @SqlUpdate("UPDATE photos SET `primary` = false WHERE countryCode = :country and stationId = :id")
    fun setAllPhotosForStationSecondary(@BindBean key: Station.Key?)

    @SqlUpdate("UPDATE photos SET `primary` = true WHERE id = :id")
    fun setPrimary(@Bind("id") id: Long)
}
