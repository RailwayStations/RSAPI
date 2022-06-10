package org.railwaystations.rsapi.adapter.in.web.controller;

import org.railwaystations.rsapi.core.ports.in.LoadPhotographersUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.validation.constraints.Size;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;

import static java.util.stream.Collectors.joining;

@RestController
@Validated
public class PhotographersController {

    private static final String COUNTRY = "country";

    @Autowired
    private LoadPhotographersUseCase loadPhotographersUseCase;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE,
            value = {"/photographers", "/photographers.json"})
    public Map<String, Long> get(@RequestParam(value = COUNTRY, required = false) @Size(min = 2, max = 2) String country) {
        return getWithCountry(country);
    }

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE, value = "/photographers.txt")
    public ResponseEntity<StreamingResponseBody> getAsText(@RequestParam(value = COUNTRY, required = false) @Size(min = 2, max = 2) String country) {
        return toCsv(getWithCountry(country));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE,
            value = {"/{country}/photographers", "/{country}/photographers.json"})
    public Map<String, Long> getWithCountry(@PathVariable(value = COUNTRY, required = false) @Size(min = 2, max = 2) String country) {
        return loadPhotographersUseCase.getPhotographersPhotocountMap(country);
    }

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE, value = "/{country}/photographers.txt")
    public ResponseEntity<StreamingResponseBody> getWithCountryAsText(@PathVariable(value = COUNTRY, required = false) @Size(min = 2, max = 2) String country) {
        return toCsv(loadPhotographersUseCase.getPhotographersPhotocountMap(country));
    }

    private ResponseEntity<StreamingResponseBody> toCsv(Map<String, Long> stringLongMap) {
        StreamingResponseBody stream = output -> {
            Writer writer = new BufferedWriter(new OutputStreamWriter(output));
            writer.write("count\tphotographer\n" +
                    stringLongMap.entrySet().stream()
                            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                            .map(this::photographerToCsv)
                            .collect(joining("\n"))
                    + "\n");
            writer.flush();
        };
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(stream);
    }

    private String photographerToCsv(Map.Entry<String, Long> photographer) {
        return String.join("\t", Long.toString(photographer.getValue()), photographer.getKey());
    }

}
