package org.railwaystations.rsapi.adapter.web.resources;

import org.railwaystations.rsapi.services.PhotoStationsService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class PhotographersResource {

    private static final String COUNTRY = "country";

    private final PhotoStationsService photoStationsService;

    public PhotographersResource(final PhotoStationsService photoStationsService) {
        this.photoStationsService = photoStationsService;
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8", MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8"}, value = "/photographers")
    public Map<String, Long> get(@RequestParam(value = COUNTRY, required = false) final String country) {
        return getWithCountry(country);
    }

    @GetMapping(produces = {MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8"}, value = "/photographers.txt")
    public Map<String, Long> getAsText(@RequestParam(value = COUNTRY, required = false) final String country) {
        return getWithCountry(country);
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"}, value = "/photographers.json")
    public Map<String, Long> getAsJson(@RequestParam(value = COUNTRY, required = false) final String country) {
        return getWithCountry(country);
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8", MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8"}, value = "/{country}/photographers")
    public Map<String, Long> getWithCountry(@PathVariable(value = COUNTRY, required = false) final String country) {
        return photoStationsService.getPhotographerMap(country);
    }

    @GetMapping(produces = {MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8"}, value = "/{country}/photographers.txt")
    public Map<String, Long> getWithCountryAsText(@PathVariable(value = COUNTRY, required = false) final String country) {
        return photoStationsService.getPhotographerMap(country);
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"}, value = "/{country}/photographers.json")
    public Map<String, Long> getWithCountryAsJson(@PathVariable(value = COUNTRY, required = false) final String country) {
        return photoStationsService.getPhotographerMap(country);
    }

}
