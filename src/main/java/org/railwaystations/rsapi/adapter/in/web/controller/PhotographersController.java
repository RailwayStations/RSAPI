package org.railwaystations.rsapi.adapter.in.web.controller;

import org.railwaystations.rsapi.adapter.in.web.api.PhotographersApi;
import org.railwaystations.rsapi.core.ports.in.LoadPhotographersUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PhotographersController implements PhotographersApi {

    private final LoadPhotographersUseCase loadPhotographersUseCase;

    public PhotographersController(LoadPhotographersUseCase loadPhotographersUseCase) {
        this.loadPhotographersUseCase = loadPhotographersUseCase;
    }

    @Override
    public ResponseEntity<Object> countryPhotographersGet(String country) {
        return ResponseEntity.ok(loadPhotographersUseCase.getPhotographersPhotocountMap(country));
    }

    @Override
    public ResponseEntity<Object> photographersGet(String country) {
        return ResponseEntity.ok(loadPhotographersUseCase.getPhotographersPhotocountMap(country));
    }

}
