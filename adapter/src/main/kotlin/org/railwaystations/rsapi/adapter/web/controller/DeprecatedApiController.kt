package org.railwaystations.rsapi.adapter.web.controller

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.railwaystations.rsapi.adapter.web.RequestUtil
import org.railwaystations.rsapi.adapter.web.model.CountryDto
import org.railwaystations.rsapi.adapter.web.model.RegisterProfileDto
import org.railwaystations.rsapi.core.model.Country
import org.railwaystations.rsapi.core.model.Station
import org.railwaystations.rsapi.core.model.User
import org.railwaystations.rsapi.core.ports.FindPhotoStationsUseCase
import org.railwaystations.rsapi.core.ports.ListCountriesUseCase
import org.railwaystations.rsapi.core.ports.ManageProfileUseCase
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.LocaleResolver
import java.util.*

@Validated
@Controller
@Deprecated("")
class DeprecatedApiController(
    private val findPhotoStationsUseCase: FindPhotoStationsUseCase,
    private val listCountriesUseCase: ListCountriesUseCase,
    private val manageProfileUseCase: ManageProfileUseCase,
    private val localeResolver: LocaleResolver,
    @Value("\${photoBaseUrl}") private val photoBaseUrl: String,
    private val requestUtil: RequestUtil,
) {

    @Deprecated("")
    @RequestMapping(method = [RequestMethod.GET], value = ["/{country}/stations"], produces = ["application/json"])
    fun countryStationsGet(
        @PathVariable("country") country: @Size(min = 2, max = 2) String,
        @RequestParam(value = "hasPhoto", required = false) hasPhoto: @Valid Boolean?,
        @RequestParam(value = "photographer", required = false) photographer: @Valid String?,
        @RequestParam(value = "active", required = false) active: @Valid Boolean?
    ): ResponseEntity<List<StationDto>> {
        return stationsGet(listOf<@Valid String>(country), hasPhoto, photographer, active)
    }

    @Deprecated("")
    @RequestMapping(method = [RequestMethod.GET], value = ["/{country}/stations/{id}"], produces = ["application/json"])
    fun countryStationsIdGet(
        @PathVariable("country") country: @Size(min = 2, max = 2) String,
        @PathVariable("id") id: String
    ): ResponseEntity<StationDto> {
        val station = findPhotoStationsUseCase.findByCountryAndId(country, id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        return ResponseEntity.ok()
            .headers(createDeprecationHeader())
            .body(station.toDto(photoBaseUrl))
    }

    private fun createDeprecationHeader(): HttpHeaders {
        val responseHeaders = HttpHeaders()
        responseHeaders["Deprecation"] = "@1661983200"
        return responseHeaders
    }

    @Deprecated("")
    @RequestMapping(method = [RequestMethod.GET], value = ["/stations"], produces = ["application/json"])
    fun stationsGet(
        @RequestParam(value = "country", required = false) country: @Valid List<String>?,
        @RequestParam(value = "hasPhoto", required = false) hasPhoto: @Valid Boolean?,
        @RequestParam(value = "photographer", required = false) photographer: @Valid String?,
        @RequestParam(value = "active", required = false) active: @Valid Boolean?
    ): ResponseEntity<List<StationDto>> {
        return ResponseEntity.ok()
            .headers(createDeprecationHeader())
            .body(
                findPhotoStationsUseCase.findByCountry(
                    country?.take(3)?.toSet() ?: setOf("de"),
                    hasPhoto,
                    photographer,
                    active
                ).map { it.toDto(photoBaseUrl) }
            )
    }

    @Deprecated("")
    @RequestMapping(method = [RequestMethod.GET], value = ["/countries.json"], produces = ["application/json"])
    fun countriesJsonGet(
        @RequestParam(value = "onlyActive", required = false) onlyActive: @Valid Boolean?
    ): ResponseEntity<List<CountryDto>> {
        return ResponseEntity.ok()
            .headers(createDeprecationHeader())
            .body(listCountriesUseCase.list(onlyActive).map(Country::toDto))
    }

    @Deprecated("")
    @RequestMapping(
        method = [RequestMethod.POST],
        value = ["/registration"],
        produces = ["text/plain"],
        consumes = ["application/json"]
    )
    fun registrationPost(
        @RequestBody registration: @Valid RegisterProfileDto
    ): ResponseEntity<Void> {
        manageProfileUseCase.register(
            registration.toDomain(localeResolver.resolveLocale(requestUtil.request)),
            requestUtil.userAgent
        )
        return ResponseEntity.accepted()
            .headers(createDeprecationHeader())
            .build()
    }

    @JsonTypeName("Station")
    @Deprecated("")
    data class StationDto(
        val idStr: String,
        val id: Long,
        val country: String,
        val title: String,
        val photographer: String?,
        val photographerUrl: String?,
        val photoUrl: String?,
        val photoId: Long?,
        val license: String?,
        val licenseUrl: String?,
        val lat: Double,
        val lon: Double,
        val createdAt: Long?,
        @JsonProperty("DS100") val ds100: String?,
        val active: Boolean,
        val outdated: Boolean?,
    )
}

private fun Station.toDto(photoBaseUrl: String) =
    DeprecatedApiController.StationDto(
        idStr = key.id,
        id = key.legacyStationId(),
        country = key.country,
        title = title,
        photographer = primaryPhoto?.photographer?.displayName,
        photographerUrl = primaryPhoto?.photographer?.displayUrl,
        photoUrl = primaryPhoto?.let { photoBaseUrl + it.urlPath },
        photoId = primaryPhoto?.id,
        license = primaryPhoto?.license?.displayName,
        licenseUrl = primaryPhoto?.license?.url,
        lat = coordinates.lat,
        lon = coordinates.lon,
        createdAt = primaryPhoto?.createdAt?.toEpochMilli(),
        ds100 = ds100,
        active = active,
        outdated = primaryPhoto?.outdated,
    )

private fun Station.Key.legacyStationId() = try {
    id.toLong()
} catch (ignored: NumberFormatException) {
    -1
}

private fun RegisterProfileDto.toDomain(locale: Locale) = User(
    name = nickname,
    email = email,
    url = link?.toString(),
    ownPhotos = photoOwner,
    anonymous = anonymous ?: false,
    license = license.toDomain(),
    sendNotifications = sendNotifications ?: true,
    newPassword = newPassword,
    locale = locale,
)
