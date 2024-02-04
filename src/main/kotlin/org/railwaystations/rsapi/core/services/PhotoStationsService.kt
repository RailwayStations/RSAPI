package org.railwaystations.rsapi.core.services

import org.apache.commons.lang3.StringUtils
import org.railwaystations.rsapi.adapter.db.StationDao
import org.railwaystations.rsapi.core.model.Station
import org.railwaystations.rsapi.core.ports.FindPhotoStationsUseCase
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class PhotoStationsService(private val stationDao: StationDao) : FindPhotoStationsUseCase {
    override fun findByCountryAndId(country: String?, stationId: String?): Station? {
        if (StringUtils.isBlank(stationId) || StringUtils.isBlank(country)) {
            return null
        }

        val key = Station.Key(country!!, stationId!!)
        return stationDao.findByKey(key.country, key.id)
    }

    override fun findRecentImports(sinceHours: Long): Set<Station> {
        return stationDao.findRecentImports(Instant.now().minus(sinceHours, ChronoUnit.HOURS))
    }

    override fun findByPhotographer(photographer: String, country: String?): Set<Station> {
        return stationDao.findByPhotographer(photographer, country)
    }

    override fun findByCountry(countries: Set<String>, hasPhoto: Boolean?, active: Boolean?): Set<Station> {
        return stationDao.findByCountryCodes(countries, hasPhoto, active)
    }

    override fun findByCountry(
        countries: Set<String>,
        hasPhoto: Boolean?,
        photographer: String?,
        active: Boolean?
    ): Set<Station> {
        val stations = findByCountry(countries, hasPhoto, active)

        if (photographer == null) {
            return stations
        }

        // TODO: can we search this on the DB?
        return stations.filter { station: Station -> station.appliesTo(photographer) }.toSet()
    }
}
