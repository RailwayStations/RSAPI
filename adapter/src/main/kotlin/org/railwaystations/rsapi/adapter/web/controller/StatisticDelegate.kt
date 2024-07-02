package org.railwaystations.rsapi.adapter.web.controller

import org.railwaystations.rsapi.adapter.web.api.StatisticApiDelegate
import org.railwaystations.rsapi.adapter.web.model.StatisticDto
import org.railwaystations.rsapi.core.ports.inbound.GetStatisticUseCase
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

@Component
class StatisticDelegate(private val getStatisticUseCase: GetStatisticUseCase) : StatisticApiDelegate {

    override fun getStats(country: String?): ResponseEntity<StatisticDto> {
        return ResponseEntity.ok(getStatisticUseCase.getStatistic(country).toDto())
    }
}