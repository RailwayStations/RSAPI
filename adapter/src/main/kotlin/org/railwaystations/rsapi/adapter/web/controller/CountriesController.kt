package org.railwaystations.rsapi.adapter.web.controller

import jakarta.validation.Valid
import org.railwaystations.openapi.api.CountriesApi
import org.railwaystations.openapi.model.CountryDto
import org.railwaystations.rsapi.core.ports.inbound.ListCountriesUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class CountriesController(private val listCountriesUseCase: ListCountriesUseCase) : CountriesApi {

    override fun getCountries(
        @Valid @RequestParam(
            value = "onlyActive",
            required = false
        ) onlyActive: Boolean?
    ): ResponseEntity<List<CountryDto>> {
        return ResponseEntity.ok(
            listCountriesUseCase.list(onlyActive).map { it.toDto() }
        )
    }

}
