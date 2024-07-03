package org.railwaystations.rsapi.adapter.web.controller

import org.railwaystations.rsapi.adapter.web.api.CountriesApi
import org.railwaystations.rsapi.adapter.web.model.CountryDto
import org.railwaystations.rsapi.adapter.web.model.ProviderAppDto
import org.railwaystations.rsapi.core.model.Country
import org.railwaystations.rsapi.core.model.ProviderApp
import org.railwaystations.rsapi.core.ports.inbound.ListCountriesUseCase
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
class CountriesController(private val listCountriesUseCase: ListCountriesUseCase) : CountriesApi {

    override fun getCountries(onlyActive: Boolean?): ResponseEntity<List<CountryDto>> {
        return ResponseEntity.ok(
            listCountriesUseCase.list(onlyActive).map { it.toDto() }
        )
    }

}

fun Country.toDto() = CountryDto(
    code = code,
    name = name,
    active = active,
    email = email,
    overrideLicense = overrideLicense?.displayName,
    timetableUrlTemplate = timetableUrlTemplate,
    providerApps = providerApps.toDtos()
)

private fun List<ProviderApp>.toDtos() = map {
    ProviderAppDto(
        type = it.type.toProviderAppType(),
        name = it.name,
        url = it.url
    )
}

private fun String.toProviderAppType() = when (this) {
    "android" -> ProviderAppDto.Type.ANDROID
    "ios" -> ProviderAppDto.Type.IOS
    "web" -> ProviderAppDto.Type.WEB
    else -> throw IllegalStateException("Unknown ProviderApp type: $this")
}
