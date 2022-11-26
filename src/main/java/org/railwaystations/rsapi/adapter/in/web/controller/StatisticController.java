package org.railwaystations.rsapi.adapter.in.web.controller;

import jakarta.validation.constraints.Size;
import org.railwaystations.rsapi.adapter.in.web.model.StatisticDto;
import org.railwaystations.rsapi.core.model.Statistic;
import org.railwaystations.rsapi.core.ports.in.GetStatisticUseCase;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class StatisticController {

    private static final String COUNTRY = "country";

    private final GetStatisticUseCase getStatisticUseCase;

    public StatisticController(GetStatisticUseCase getStatisticUseCase) {
        this.getStatisticUseCase = getStatisticUseCase;
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE}, value = "/stats")
    public StatisticDto get(@Nullable @RequestParam(value = StatisticController.COUNTRY, required = false) @Size(min = 2, max = 2) String country) {
        return getWithCountry(country);
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE}, value = "/stats.json")
    public StatisticDto getAsJson(@Nullable @RequestParam(value = StatisticController.COUNTRY, required = false) @Size(min = 2, max = 2) String country) {
        return getWithCountry(country);
    }

    @GetMapping(produces = {MediaType.TEXT_PLAIN_VALUE}, value = "/stats.txt")
    public StatisticDto getAsText(@Nullable @RequestParam(value = StatisticController.COUNTRY, required = false) @Size(min = 2, max = 2) String country) {
        return getWithCountry(country);
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE}, value = "/{country}/stats")
    public StatisticDto getWithCountry(@PathVariable(StatisticController.COUNTRY) @Size(min = 2, max = 2) String country) {
        return getStatisticMap(country);
    }

    @GetMapping(produces = {MediaType.TEXT_PLAIN_VALUE}, value = "/{country}/stats.txt")
    public StatisticDto getWithCountryAsText(@PathVariable(StatisticController.COUNTRY) @Size(min = 2, max = 2) String country) {
        return getStatisticMap(country);
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE}, value = "/{country}/stats.json")
    public StatisticDto getWithCountryAsJson(@PathVariable(StatisticController.COUNTRY) @Size(min = 2, max = 2) String country) {
        return getStatisticMap(country);
    }

    private StatisticDto getStatisticMap(String country) {
        return toDto(getStatisticUseCase.getStatistic(country));
    }

    private StatisticDto toDto(Statistic statistic) {
        return new StatisticDto()
                .countryCode(statistic.countryCode())
                .photographers(statistic.photographers())
                .total(statistic.total())
                .withoutPhoto(statistic.withoutPhoto())
                .withPhoto(statistic.withPhoto());
    }

}
