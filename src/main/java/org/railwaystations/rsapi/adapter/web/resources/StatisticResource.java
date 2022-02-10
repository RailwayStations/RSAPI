package org.railwaystations.rsapi.adapter.web.resources;

import org.railwaystations.rsapi.domain.model.Statistic;
import org.railwaystations.rsapi.services.PhotoStationsService;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatisticResource {

    private static final String COUNTRY = "country";

    private final PhotoStationsService photoStationsService;

    public StatisticResource(final PhotoStationsService photoStationsService) {
        this.photoStationsService = photoStationsService;
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE}, value = "/stats")
    public Statistic get(@Nullable @RequestParam(StatisticResource.COUNTRY) final String country) {
        return getWithCountry(country);
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE}, value = "/stats.json")
    public Statistic getAsJson(@Nullable @RequestParam(StatisticResource.COUNTRY) final String country) {
        return getWithCountry(country);
    }

    @GetMapping(produces = {MediaType.TEXT_PLAIN_VALUE}, value = "/stats.txt")
    public Statistic getAsText(@Nullable @RequestParam(StatisticResource.COUNTRY) final String country) {
        return getWithCountry(country);
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE}, value = "/{country}/stats")
    public Statistic getWithCountry(@PathVariable(StatisticResource.COUNTRY) final String country) {
        return getStatisticMap(country);
    }

    @GetMapping(produces = {MediaType.TEXT_PLAIN_VALUE}, value = "/{country}/stats.txt")
    public Statistic getWithCountryAsText(@PathVariable(StatisticResource.COUNTRY) final String country) {
        return getStatisticMap(country);
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE}, value = "/{country}/stats.json")
    public Statistic getWithCountryAsJson(@PathVariable(StatisticResource.COUNTRY) final String country) {
        return getStatisticMap(country);
    }

    private Statistic getStatisticMap(final String country) {
        return photoStationsService.getStatistic(country);
    }

}
