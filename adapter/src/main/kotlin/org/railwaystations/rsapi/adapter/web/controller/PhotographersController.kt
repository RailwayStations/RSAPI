package org.railwaystations.rsapi.adapter.web.controller

import org.railwaystations.rsapi.adapter.web.api.PhotographersApi
import org.railwaystations.rsapi.core.ports.inbound.LoadPhotographersUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class PhotographersController(private val loadPhotographersUseCase: LoadPhotographersUseCase) : PhotographersApi {

    override fun getPhotographers(country: String?): ResponseEntity<Any> {
        return ResponseEntity.ok(loadPhotographersUseCase.getPhotographersPhotocountMap(country))
    }

}
