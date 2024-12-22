package org.railwaystations.rsapi.adapter.web.controller

import org.railwaystations.rsapi.adapter.web.api.StatisticApi
import org.railwaystations.rsapi.adapter.web.model.StatisticDto
import org.railwaystations.rsapi.core.model.Statistic
import org.railwaystations.rsapi.core.ports.inbound.GetStatisticUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class StatisticController(private val getStatisticUseCase: GetStatisticUseCase) : StatisticApi {

    override fun getStats(country: String?): ResponseEntity<StatisticDto> {
        return ResponseEntity.ok(getStatisticUseCase.getStatistic(country).toDto())
    }

}

private fun Statistic.toDto() = StatisticDto(
    total = total.toLong(),
    withPhoto = withPhoto.toLong(),
    withoutPhoto = withoutPhoto.toLong(),
    photographers = photographers.toLong(),
    countryCode = countryCode
)
