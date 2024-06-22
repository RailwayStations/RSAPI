package org.railwaystations.rsapi.adapter.web.controller

import org.railwaystations.openapi.api.CountriesApiDelegate
import org.railwaystations.openapi.model.CountryDto
import org.railwaystations.openapi.model.ProviderAppDto
import org.railwaystations.rsapi.core.model.Country
import org.railwaystations.rsapi.core.model.ProviderApp
import org.railwaystations.rsapi.core.ports.inbound.ListCountriesUseCase
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

@Component
class CountriesDelegate(private val listCountriesUseCase: ListCountriesUseCase) : CountriesApiDelegate {

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
