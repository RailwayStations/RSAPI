package org.railwaystations.rsapi.adapter.web.controller

import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.railwaystations.rsapi.core.ports.LoadPhotographersUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class PhotographersController(private val loadPhotographersUseCase: LoadPhotographersUseCase) {
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/photographers"],
        produces = ["application/json"]
    )
    fun photographersGet(
        @Size(max = 2, min = 2) @Valid @RequestParam(
            required = false,
            value = "country"
        ) country: String?
    ): ResponseEntity<Any> {
        return ResponseEntity.ok(loadPhotographersUseCase.getPhotographersPhotocountMap(country))
    }
}
