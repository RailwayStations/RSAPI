package org.railwaystations.rsapi.adapter.web.controller

import jakarta.validation.Valid
import org.railwaystations.rsapi.adapter.web.model.CountryDto
import org.railwaystations.rsapi.adapter.web.model.ProviderAppDto
import org.railwaystations.rsapi.core.model.Country
import org.railwaystations.rsapi.core.model.ProviderApp
import org.railwaystations.rsapi.core.ports.ListCountriesUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class CountriesController(private val listCountriesUseCase: ListCountriesUseCase) {
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/countries"],
        produces = ["application/json"]
    )
    fun countriesGet(
        @Valid @RequestParam(
            required = false,
            value = "onlyActive"
        ) onlyActive: Boolean?
    ): ResponseEntity<List<CountryDto>> {
        return ResponseEntity.ok(
            listCountriesUseCase.list(onlyActive).map { country -> toDto(country) }
        )
    }

    companion object {
        fun toDto(country: Country): CountryDto {
            return CountryDto(
                code = country.code,
                name = country.name,
                active = country.active,
                email = country.email,
                overrideLicense = country.overrideLicense?.displayName,
                timetableUrlTemplate = country.timetableUrlTemplate,
                providerApps = toDto(country.providerApps)
            )
        }

        private fun toDto(providerApps: List<ProviderApp>): List<ProviderAppDto> {
            return providerApps
                .map { p ->
                    ProviderAppDto(
                        type = mapProviderAppType(p.type),
                        name = p.name,
                        url = p.url
                    )
                }
        }

        private fun mapProviderAppType(type: String): ProviderAppDto.Type {
            return when (type) {
                "android" -> ProviderAppDto.Type.ANDROID
                "ios" -> ProviderAppDto.Type.IOS
                "web" -> ProviderAppDto.Type.WEB
                else -> throw IllegalStateException("Unknown ProviderApp type: $type")
            }
        }
    }
}
