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

    override val countryStatisticMessage: String
        get() = "Countries statistic: \n" + countryPort.list(true)
            .sortedBy { it.code }
            .map { country -> getStatistic(country.code) }
            .joinToString("\n") { statistic -> "- ${statistic.countryCode}: ${statistic.withPhoto} of ${statistic.total}" }

    override fun getStatistic(country: String?): Statistic {
        require(!(country != null && countryPort.findById(country) == null)) { "Country $country does not exist" }
        return stationPort.getStatistic(country)
    }
}
