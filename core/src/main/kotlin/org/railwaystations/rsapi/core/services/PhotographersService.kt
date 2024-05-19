package org.railwaystations.rsapi.core.services

import org.railwaystations.rsapi.core.ports.inbound.LoadPhotographersUseCase
import org.railwaystations.rsapi.core.ports.outbound.CountryPort
import org.railwaystations.rsapi.core.ports.outbound.StationPort
import org.springframework.stereotype.Service

@Service
class PhotographersService(
    private val stationPort: StationPort,
    private val countryPort: CountryPort,
) : LoadPhotographersUseCase {

    override fun getPhotographersPhotocountMap(country: String?): Map<String, Long> {
        require(!(country != null && countryPort.findById(country) == null)) { "Country $country does not exist" }
        return stationPort.getPhotographerMap(country)
    }
}
