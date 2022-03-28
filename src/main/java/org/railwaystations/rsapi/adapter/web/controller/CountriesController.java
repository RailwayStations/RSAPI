package org.railwaystations.rsapi.adapter.web.controller;

import org.railwaystations.rsapi.core.model.Country;
import org.railwaystations.rsapi.core.services.CountryService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
public class CountriesController {

    private static final String ONLY_ACTIVE = "onlyActive";
    private final CountryService countryService;

    public CountriesController(final CountryService countryService) {
        this.countryService = countryService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = {"/countries", "/countries.json"})
    public Collection<Country> list(@RequestParam(value = ONLY_ACTIVE, required = false) final Boolean onlyActive) {
        return countryService.list(onlyActive);
    }

}
