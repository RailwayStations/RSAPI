package org.railwaystations.rsapi.adapter.in.web.controller;

import org.railwaystations.rsapi.core.model.Country;
import org.railwaystations.rsapi.core.ports.in.ListCountriesUseCase;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
public class CountriesController {

    private static final String ONLY_ACTIVE = "onlyActive";
    private final ListCountriesUseCase listCountriesUseCase;

    public CountriesController(final ListCountriesUseCase listCountriesUseCase) {
        this.listCountriesUseCase = listCountriesUseCase;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = {"/countries", "/countries.json"})
    public Collection<Country> list(@RequestParam(value = ONLY_ACTIVE, required = false) final Boolean onlyActive) {
        return listCountriesUseCase.list(onlyActive);
    }

}
