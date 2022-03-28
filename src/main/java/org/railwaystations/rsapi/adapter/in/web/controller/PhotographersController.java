package org.railwaystations.rsapi.adapter.in.web.controller;

import org.railwaystations.rsapi.core.ports.in.LoadPhotographersUseCase;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Size;
import java.util.Map;

@RestController
@Validated
public class PhotographersController {

    private static final String COUNTRY = "country";

    private final LoadPhotographersUseCase loadPhotographersUseCase;

    public PhotographersController(final LoadPhotographersUseCase loadPhotographersUseCase) {
        this.loadPhotographersUseCase = loadPhotographersUseCase;
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"},
            value = {"/photographers", "/photographers.json"})
    public Map<String, Long> get(@RequestParam(value = COUNTRY, required = false) @Size(min = 2, max = 2) final String country) {
        return getWithCountry(country);
    }

    @GetMapping(produces = {MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8"}, value = "/photographers.txt")
    public Map<String, Long> getAsText(@RequestParam(value = COUNTRY, required = false) @Size(min = 2, max = 2) final String country) {
        return getWithCountry(country);
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"},
            value = {"/{country}/photographers", "/{country}/photographers.json"})
    public Map<String, Long> getWithCountry(@PathVariable(value = COUNTRY, required = false) @Size(min = 2, max = 2) final String country) {
        return loadPhotographersUseCase.getPhotographersPhotocountMap(country);
    }

    @GetMapping(produces = {MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8"}, value = "/{country}/photographers.txt")
    public Map<String, Long> getWithCountryAsText(@PathVariable(value = COUNTRY, required = false) @Size(min = 2, max = 2) final String country) {
        return loadPhotographersUseCase.getPhotographersPhotocountMap(country);
    }

}
