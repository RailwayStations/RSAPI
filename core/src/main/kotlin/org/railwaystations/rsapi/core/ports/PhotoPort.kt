package org.railwaystations.rsapi.core.ports

import org.railwaystations.rsapi.core.model.Photo
import org.railwaystations.rsapi.core.model.Station

interface PhotoPort {
    fun insert(photo: Photo?): Long
    fun update(photo: Photo?)
    fun delete(id: Long)
    fun updatePhotoOutdated(id: Long)
    fun setAllPhotosForStationSecondary(key: Station.Key?)
    fun setPrimary(id: Long)
}
