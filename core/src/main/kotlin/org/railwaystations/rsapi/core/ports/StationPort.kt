package org.railwaystations.rsapi.core.ports

import org.railwaystations.rsapi.core.model.Coordinates
import org.railwaystations.rsapi.core.model.Station
import org.railwaystations.rsapi.core.model.Statistic
import java.time.Instant

interface StationPort {
    fun findByCountryCodes(countryCodes: Set<String>, hasPhoto: Boolean?, active: Boolean?): Set<Station>
    fun findByKey(countryCode: String, id: String): Station?
    fun findByPhotographer(photographer: String, countryCode: String?): Set<Station>
    fun findRecentImports(since: Instant): Set<Station>
    fun getStatistic(countryCode: String?): Statistic
    fun getPhotographerMap(countryCode: String?): Map<String, Long>
    fun insert(station: Station)
    fun delete(key: Station.Key)
    fun updateActive(key: Station.Key, active: Boolean)
    fun countNearbyCoordinates(coordinates: Coordinates): Int
    val maxZ: Int
    fun changeStationTitle(key: Station.Key, newTitle: String)
    fun updateLocation(key: Station.Key, coordinates: Coordinates)
}
