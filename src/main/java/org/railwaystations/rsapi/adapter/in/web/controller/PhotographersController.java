package org.railwaystations.rsapi.adapter.in.web.controller;

import lombok.RequiredArgsConstructor;
import org.railwaystations.rsapi.adapter.in.web.api.PhotographersApi;
import org.railwaystations.rsapi.core.ports.in.LoadPhotographersUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PhotographersController implements PhotographersApi {

    private final LoadPhotographersUseCase loadPhotographersUseCase;

    @Override
    public ResponseEntity<Object> photographersGet(String country) {
        return ResponseEntity.ok(loadPhotographersUseCase.getPhotographersPhotocountMap(country));
    }

}
