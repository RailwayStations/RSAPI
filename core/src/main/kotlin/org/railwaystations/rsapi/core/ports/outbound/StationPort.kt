package org.railwaystations.rsapi.core.ports.outbound

import org.railwaystations.rsapi.core.model.Coordinates
import org.railwaystations.rsapi.core.model.Station
import org.railwaystations.rsapi.core.model.Statistic
import java.time.Instant

interface StationPort {
    fun findByCountryCodes(countryCodes: Set<String>, hasPhoto: Boolean? = null, active: Boolean? = null): Set<Station>
    fun findByKey(key: Station.Key): Station?
    fun findByPhotographer(photographer: String, countryCode: String? = null): Set<Station>
    fun findRecentImports(since: Instant): Set<Station>
    fun getStatistic(countryCode: String? = null): Statistic
    fun getPhotographerMap(countryCode: String? = null): Map<String, Int>
    fun insert(station: Station)
    fun delete(key: Station.Key)
    fun updateActive(key: Station.Key, active: Boolean)
    fun countNearbyCoordinates(coordinates: Coordinates): Int
    fun maxZ(): Int
    fun changeStationTitle(key: Station.Key, newTitle: String)
    fun updateLocation(key: Station.Key, coordinates: Coordinates)
    fun findByPhotoId(photoId: Long): Station
}