package org.railwaystations.rsapi.adapter.in.web.controller;

import lombok.RequiredArgsConstructor;
import org.railwaystations.rsapi.adapter.in.web.api.CountriesApi;
import org.railwaystations.rsapi.adapter.in.web.model.CountryDto;
import org.railwaystations.rsapi.adapter.in.web.model.ProviderAppDto;
import org.railwaystations.rsapi.core.model.Country;
import org.railwaystations.rsapi.core.model.ProviderApp;
import org.railwaystations.rsapi.core.ports.in.ListCountriesUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CountriesController implements CountriesApi {

    private final ListCountriesUseCase listCountriesUseCase;

    static CountryDto toDto(Country country) {
        return new CountryDto(country.getCode(), country.getName(), country.isActive())
                .email(country.getEmail())
                .overrideLicense(country.getOverrideLicense() != null ? country.getOverrideLicense().getDisplayName() : null)
                .timetableUrlTemplate(country.getTimetableUrlTemplate())
                .providerApps(toDto(country.getProviderApps()));
    }

    static List<ProviderAppDto> toDto(List<ProviderApp> providerApps) {
        return providerApps.stream()
                .map(p -> new ProviderAppDto(ProviderAppDto.TypeEnum.fromValue(p.getType()), p.getName(), p.getUrl()))
                .toList();
    }

    @Override
    public ResponseEntity<List<CountryDto>> countriesGet(Boolean onlyActive) {
        return ResponseEntity.ok(listCountriesUseCase.list(onlyActive).stream().map(CountriesController::toDto).toList());
    }

}
