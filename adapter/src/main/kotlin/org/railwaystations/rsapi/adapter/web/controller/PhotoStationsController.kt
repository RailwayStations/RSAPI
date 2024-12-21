package org.railwaystations.rsapi.adapter.web.controller

import org.railwaystations.rsapi.adapter.web.api.PhotoStationsApi
import org.railwaystations.rsapi.adapter.web.model.PhotoDto
import org.railwaystations.rsapi.adapter.web.model.PhotoLicenseDto
import org.railwaystations.rsapi.adapter.web.model.PhotoStationDto
import org.railwaystations.rsapi.adapter.web.model.PhotoStationsDto
import org.railwaystations.rsapi.adapter.web.model.PhotographerDto
import org.railwaystations.rsapi.core.model.Station
import org.railwaystations.rsapi.core.ports.inbound.FindPhotoStationsUseCase
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.net.URI

@RestController
@Validated
class PhotoStationsController(
    private val findPhotoStationsUseCase: FindPhotoStationsUseCase,
    @Value("\${photoBaseUrl}") private val photoBaseUrl: String,
) : PhotoStationsApi {

    override fun getPhotoStationById(country: String, id: String): ResponseEntity<PhotoStationsDto> {
        val stations = setOf(
            findPhotoStationsUseCase.findByKey(Station.Key(country, id))
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        )
        return ResponseEntity.ok(stations.toDto(photoBaseUrl))
    }

    override fun getPhotoStationByCountry(
        country: String,
        hasPhoto: Boolean?,
        isActive: Boolean?
    ): ResponseEntity<PhotoStationsDto> {
        val stations =
            findPhotoStationsUseCase.findByCountry(mutableSetOf(country), hasPhoto, isActive)
        return ResponseEntity.ok(stations.toDto(photoBaseUrl))
    }

    override fun getPhotoStationsByPhotographer(
        photographer: String,
        country: String?
    ): ResponseEntity<PhotoStationsDto> {
        val stations = findPhotoStationsUseCase.findByPhotographer(photographer, country)
        return ResponseEntity.ok(stations.toDto(photoBaseUrl))
    }

    override fun getPhotoStationsByRecentPhotoImports(sinceHours: Int): ResponseEntity<PhotoStationsDto> {
        val stations = findPhotoStationsUseCase.findRecentImports(sinceHours.toLong())
        return ResponseEntity.ok(stations.toDto(photoBaseUrl))
    }

}

private fun Set<Station>.toDto(photoBaseUrl: String) =
    PhotoStationsDto(photoBaseUrl, toLicenses(), toPhotographers(), toStations())

private fun Set<Station>.toStations() =
    map { station ->
        PhotoStationDto(
            country = station.key.country,
            id = station.key.id,
            title = station.title,
            lat = station.coordinates.lat,
            lon = station.coordinates.lon,
            photos = station.toPhotoDtos(),
            shortCode = station.ds100,
            inactive = if (station.active) null else true,
        )
    }

private fun Station.toPhotoDtos() =
    photos
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

private fun Set<Station>.toPhotographers() =
    flatMap { station -> station.photos }
        .map { photo -> photo.photographer }
        .associateBy { user -> user.displayName }
        .map { entry ->
            PhotographerDto(name = entry.value.displayName, url = URI.create(entry.value.displayUrl))
        }

private fun Set<Station>.toLicenses() =
    asSequence()
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
