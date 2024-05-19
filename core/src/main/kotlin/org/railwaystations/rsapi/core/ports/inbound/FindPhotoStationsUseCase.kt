package org.railwaystations.rsapi.core.ports.inbound

import org.railwaystations.rsapi.core.model.Station

interface FindPhotoStationsUseCase {
    fun findByCountry(
        countries: Set<String>,
        hasPhoto: Boolean?,
        photographer: String?,
        active: Boolean?
    ): Set<Station>

    fun findByCountry(countries: Set<String>, hasPhoto: Boolean?, active: Boolean?): Set<Station>

    fun findByCountryAndId(country: String?, stationId: String?): Station?

    fun findRecentImports(sinceHours: Long): Set<Station>

    fun findByPhotographer(photographer: String, country: String?): Set<Station>
}