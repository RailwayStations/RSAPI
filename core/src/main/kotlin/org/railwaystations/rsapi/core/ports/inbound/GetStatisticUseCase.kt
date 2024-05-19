package org.railwaystations.rsapi.core.ports.inbound

import org.railwaystations.rsapi.core.model.Statistic

interface GetStatisticUseCase {
    val countryStatisticMessage: String

    fun getStatistic(country: String?): Statistic
}