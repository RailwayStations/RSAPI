package org.railwaystations.rsapi.adapter.web.controller

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.railwaystations.rsapi.adapter.web.model.PhotoDto
import org.railwaystations.rsapi.adapter.web.model.PhotoLicenseDto
import org.railwaystations.rsapi.adapter.web.model.PhotoStationDto
import org.railwaystations.rsapi.adapter.web.model.PhotoStationsDto
import org.railwaystations.rsapi.adapter.web.model.PhotographerDto
import org.railwaystations.rsapi.core.model.Station
import org.railwaystations.rsapi.core.ports.FindPhotoStationsUseCase
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.net.URI

@RestController
@Validated
class PhotoStationsController(
    private val findPhotoStationsUseCase: FindPhotoStationsUseCase,
    @Value("\${photoBaseUrl}") private val photoBaseUrl: String,
) {

    private fun mapPhotoStations(stations: Set<Station>): PhotoStationsDto {
        return PhotoStationsDto(photoBaseUrl, mapLicenses(stations), mapPhotographers(stations), mapStations(stations))
    }

    private fun mapStations(stations: Set<Station>): List<PhotoStationDto> {
        return stations
            .map { station ->
                PhotoStationDto(
                    country = station.key.country,
                    id = station.key.id,
                    title = station.title,
                    lat = station.coordinates.lat,
                    lon = station.coordinates.lon,
                    photos = mapPhotos(station),
                    shortCode = station.ds100,
                    inactive = if (station.active) null else true,
                )
            }
    }

    private fun mapPhotos(station: Station): List<PhotoDto> {
        return station.photos
            .sorted()
            .map { photo ->
                PhotoDto(
                    id = photo.id,
                    photographer = photo.photographer.displayName,
                    path = photo.urlPath,
                    createdAt = photo.createdAt.toEpochMilli(),
                    license = photo.license.name,
                    outdated = if (photo.outdated) true else null
                )
            }
    }

    private fun mapPhotographers(stations: Set<Station>): List<PhotographerDto> {
        return stations
            .flatMap { station -> station.photos }
            .map { photo -> photo.photographer }
            .associateBy { user -> user.displayName }
            .map { entry ->
                PhotographerDto(name = entry.value.displayName, url = URI.create(entry.value.displayUrl))
            }
    }

    private fun mapLicenses(stations: Set<Station>): List<PhotoLicenseDto> {
        return stations
            .asSequence()
            .flatMap { station -> station.photos }
            .map { photo -> photo.license }
            .associateBy { license -> license.name }
            .filter { it.value.url != null }
            .mapNotNull { license ->
                license.value.url?.let { url ->
                    PhotoLicenseDto(
                        license.key,
                        license.value.displayName,
                        URI.create(url),
                    )
                }
            }
    }

    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/photoStationById/{country}/{id}"],
        produces = ["application/json"]
    )
    fun photoStationByIdCountryIdGet(
        @Size(
            max = 2,
            min = 2
        ) @PathVariable(value = "country") country: String,
        @PathVariable(value = "id") id: String
    ): ResponseEntity<PhotoStationsDto> {
        val stations = setOf(
            findPhotoStationsUseCase.findByCountryAndId(country, id)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        )
        return ResponseEntity.ok(mapPhotoStations(stations))
    }

    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/photoStationsByCountry/{country}"],
        produces = ["application/json"]
    )
    fun photoStationsByCountryCountryGet(
        @Size(
            max = 2,
            min = 2
        ) @PathVariable(value = "country") country: String,
        @Valid @RequestParam(
            required = false,
            value = "hasPhoto"
        ) hasPhoto: Boolean?,
        @Valid @RequestParam(
            required = false,
            value = "isActive"
        ) isActive: Boolean?
    ): ResponseEntity<PhotoStationsDto> {
        val stations =
            findPhotoStationsUseCase.findByCountry(mutableSetOf(country), hasPhoto, isActive)
        return ResponseEntity.ok(mapPhotoStations(stations))
    }

    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/photoStationsByPhotographer/{photographer}"],
        produces = ["application/json"]
    )
    fun photoStationsByPhotographerPhotographerGet(
        @PathVariable(value = "photographer") photographer: String,
        @Size(
            max = 2,
            min = 2
        ) @Valid @RequestParam(
            required = false,
            value = "country"
        ) country: String?
    ): ResponseEntity<PhotoStationsDto> {
        val stations = findPhotoStationsUseCase.findByPhotographer(photographer, country)
        return ResponseEntity.ok(mapPhotoStations(stations))
    }

    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/photoStationsByRecentPhotoImports"],
        produces = ["application/json"]
    )
    fun photoStationsByRecentPhotoImportsGet(
        @Min(1) @Max(800) @Valid @RequestParam(
            value = "sinceHours",
            required = false,
            defaultValue = "10"
        ) sinceHours: Int
    ): ResponseEntity<PhotoStationsDto> {
        val stations = findPhotoStationsUseCase.findRecentImports(sinceHours.toLong())
        return ResponseEntity.ok(mapPhotoStations(stations))
    }
}
