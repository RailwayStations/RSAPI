package org.railwaystations.rsapi.adapter.web.controller

import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.railwaystations.openapi.model.StatisticDto
import org.railwaystations.rsapi.core.model.Statistic
import org.railwaystations.rsapi.core.ports.inbound.GetStatisticUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class StatisticController(private val getStatisticUseCase: GetStatisticUseCase) {

    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/stats"],
        produces = ["application/json"]
    )
    fun statsGet(
        @Size(max = 2, min = 2) @Valid @RequestParam(
            required = false,
            value = "country"
        ) country: String?
    ): ResponseEntity<StatisticDto> {
        return ResponseEntity.ok(getStatisticUseCase.getStatistic(country).toDto())
    }
}

private fun Statistic.toDto() = StatisticDto(
    total = total,
    withPhoto = withPhoto,
    withoutPhoto = withoutPhoto,
    photographers = photographers,
    countryCode = countryCode
)
