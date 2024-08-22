package org.railwaystations.rsapi.core.services

import org.railwaystations.rsapi.core.model.Statistic
import org.railwaystations.rsapi.core.ports.inbound.GetStatisticUseCase
import org.railwaystations.rsapi.core.ports.outbound.CountryPort
import org.railwaystations.rsapi.core.ports.outbound.StationPort
import org.springframework.stereotype.Service

@Service
class StatisticService(
    private val countryPort: CountryPort,
    private val stationPort: StationPort,
) : GetStatisticUseCase {

    override fun getStatistic(country: String?): Statistic {
        require(!(country != null && countryPort.findById(country) == null)) { "Country $country does not exist" }
        return stationPort.getStatistic(country)
    }
}
