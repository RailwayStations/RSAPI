package org.railwaystations.rsapi.adapter.in.web.controller;

import org.railwaystations.rsapi.adapter.in.web.model.CountryDto;
import org.railwaystations.rsapi.adapter.in.web.model.ProviderAppDto;
import org.railwaystations.rsapi.core.model.Country;
import org.railwaystations.rsapi.core.model.ProviderApp;
import org.railwaystations.rsapi.core.ports.in.ListCountriesUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

@RestController
public class CountriesController {

    private static final String ONLY_ACTIVE = "onlyActive";

    @Autowired
    private ListCountriesUseCase listCountriesUseCase;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = {"/countries", "/countries.json"})
    public Collection<CountryDto> list(@RequestParam(value = ONLY_ACTIVE, required = false) final Boolean onlyActive) {
        return listCountriesUseCase.list(onlyActive).stream().map(this::toDto).toList();
    }

    private CountryDto toDto(final Country country) {
        return new CountryDto()
                .code(country.getCode())
                .name(country.getName())
                .active(country.isActive())
                .email(country.getEmail())
                .overrideLicense(country.getOverrideLicense())
                .timetableUrlTemplate(country.getTimetableUrlTemplate())
                .twitterTags(country.getTwitterTags())
                .providerApps(toDto(country.getProviderApps()));
    }

    private List<ProviderAppDto> toDto(final List<ProviderApp> providerApps) {
        return providerApps.stream()
                .map(p -> new ProviderAppDto().name(p.getName()).type(ProviderAppDto.TypeEnum.fromValue(p.getType())).url(p.getUrl()))
                .toList();
    }

}
