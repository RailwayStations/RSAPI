package org.railwaystations.rsapi.core.services

import org.railwaystations.rsapi.adapter.db.CountryDao
import org.railwaystations.rsapi.adapter.db.StationDao
import org.railwaystations.rsapi.core.model.Statistic
import org.railwaystations.rsapi.core.ports.GetStatisticUseCase
import org.springframework.stereotype.Service

@Service
class StatisticService(
    private val countryDao: CountryDao,
    private val stationDao: StationDao,
) : GetStatisticUseCase {

    override val countryStatisticMessage: String
        get() = "Countries statistic: \n" + countryDao.list(true)
            .sortedBy { it.code }
            .map { country -> getStatistic(country.code) }
            .joinToString("\n") { statistic -> "- ${statistic.countryCode}: ${statistic.withPhoto} of ${statistic.total}" }

    override fun getStatistic(country: String?): Statistic {
        require(!(country != null && countryDao.findById(country) == null)) { "Country $country does not exist" }
        return stationDao.getStatistic(country)
    }
}
