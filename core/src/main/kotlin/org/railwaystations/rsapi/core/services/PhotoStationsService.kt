package org.railwaystations.rsapi.core.services

import org.railwaystations.rsapi.core.model.Station
import org.railwaystations.rsapi.core.ports.inbound.FindPhotoStationsUseCase
import org.railwaystations.rsapi.core.ports.outbound.StationPort
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class PhotoStationsService(private val stationPort: StationPort) : FindPhotoStationsUseCase {
    override fun findByKey(key: Station.Key): Station? {
        return stationPort.findByKey(key)
    }

    override fun findRecentImports(sinceHours: Long): Set<Station> {
        return stationPort.findRecentImports(Instant.now().minus(sinceHours, ChronoUnit.HOURS))
    }

    override fun findByPhotographer(photographer: String, country: String?): Set<Station> {
        return stationPort.findByPhotographer(photographer, country)
    }

    override fun findByCountry(countries: Set<String>, hasPhoto: Boolean?, active: Boolean?): Set<Station> {
        return stationPort.findByCountryCodes(countries, hasPhoto, active)
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
