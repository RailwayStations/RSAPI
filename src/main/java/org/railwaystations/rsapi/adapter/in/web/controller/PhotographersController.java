package org.railwaystations.rsapi.adapter.in.web.controller;

import org.railwaystations.rsapi.core.ports.in.LoadPhotographersUseCase;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Size;
import java.util.Map;

import static java.util.stream.Collectors.joining;

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
    public @ResponseBody
    String getAsText(@RequestParam(value = COUNTRY, required = false) @Size(min = 2, max = 2) final String country) {
        return toCsv(getWithCountry(country));
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"},
            value = {"/{country}/photographers", "/{country}/photographers.json"})
    public Map<String, Long> getWithCountry(@PathVariable(value = COUNTRY, required = false) @Size(min = 2, max = 2) final String country) {
        return loadPhotographersUseCase.getPhotographersPhotocountMap(country);
    }

    @GetMapping(produces = {MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8"}, value = "/{country}/photographers.txt")
    public @ResponseBody
    String getWithCountryAsText(@PathVariable(value = COUNTRY, required = false) @Size(min = 2, max = 2) final String country) {
        return toCsv(loadPhotographersUseCase.getPhotographersPhotocountMap(country));
    }

    private String toCsv(final Map<String, Long> stringLongMap) {
        return "count\tphotographer\n" +
            stringLongMap.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .map(this::photographerToCsv)
                    .collect(joining("\n"))
                + "\n";
    }

    private String photographerToCsv(final Map.Entry<String, Long> photographer) {
        return String.join("\t", Long.toString(photographer.getValue()), photographer.getKey());
    }

}
