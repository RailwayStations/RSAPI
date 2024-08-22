package org.railwaystations.rsapi.core.ports.inbound

import org.railwaystations.rsapi.core.model.Statistic

fun interface GetStatisticUseCase {
    fun getStatistic(country: String?): Statistic
}