package org.railwaystations.rsapi.adapter.web.controller

import jakarta.servlet.http.HttpServletRequest
import org.apache.commons.io.IOUtils
import org.apache.commons.io.output.NullOutputStream
import org.apache.commons.lang3.StringUtils
import org.railwaystations.rsapi.adapter.web.InboxResponseMapper.toDto
import org.railwaystations.rsapi.adapter.web.InboxResponseMapper.toHttpStatus
import org.railwaystations.rsapi.adapter.web.RequestUtil
import org.railwaystations.rsapi.adapter.web.model.InboxResponseDto
import org.railwaystations.rsapi.app.auth.AuthUser
import org.railwaystations.rsapi.core.model.InboxResponse
import org.railwaystations.rsapi.core.model.User
import org.railwaystations.rsapi.core.ports.ManageInboxUseCase
import org.railwaystations.rsapi.core.ports.ManageProfileUseCase
import org.railwaystations.rsapi.core.utils.Logger
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.LocaleResolver
import java.io.IOException
import java.io.InputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*

@RestController
@Validated
class PhotoUploadController(
    private val manageInboxUseCase: ManageInboxUseCase,
    private val manageProfileUseCase: ManageProfileUseCase,
    private val localeResolver: LocaleResolver,
    private val requestUtil: RequestUtil,
) {

    private val log by Logger()

    /**
     * Not part of the "official" API.
     * Supports upload of photos via the website.
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping(
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE + ";charset=UTF-8"],
        value = ["/photoUploadMultipartFormdata"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    fun photoUploadMultipartFormdata(
        @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) userAgent: String,
        @AuthenticationPrincipal user: AuthUser,
        @RequestParam(value = "stationId", required = false) stationId: String?,
        @RequestParam(value = "countryCode", required = false) countryCode: String,
        @RequestParam(value = "stationTitle", required = false) stationTitle: String?,
        @RequestParam(value = "latitude", required = false) latitude: Double?,
        @RequestParam(value = "longitude", required = false) longitude: Double?,
        @RequestParam(value = "comment", required = false) comment: String?,
        @RequestParam(value = "active", required = false) active: Boolean?,
        @RequestParam(value = "file") file: MultipartFile
    ): ResponseEntity<InboxResponseDto> {
        log.info(
            "MultipartFormData2: user={}, station={}, country={}, file={}",
            user.username,
            stationId,
            countryCode,
            file.name
        )

        try {
            val response: InboxResponseDto = if (file.isEmpty) {
                uploadPhoto(
                    userAgent, null, StringUtils.trimToNull(stationId),
                    countryCode, null, stationTitle, latitude, longitude, comment, active, user
                )
            } else {
                uploadPhoto(
                    userAgent, file.inputStream, StringUtils.trimToNull(stationId),
                    countryCode, file.contentType, stationTitle, latitude, longitude, comment, active, user
                )
            }
            return ResponseEntity<InboxResponseDto>(response, response.state.toHttpStatusMultipartFormdata())
        } catch (e: Exception) {
            log.error("FormUpload error", e)
            return ResponseEntity<InboxResponseDto>(
                InboxResponseDto(InboxResponseDto.State.ERROR),
                toHttpStatus(InboxResponseDto.State.ERROR)
            )
        }
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping(
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
        value = ["/photoUpload"]
    )
    @Throws(
        IOException::class
    )
    fun photoUpload(
        request: HttpServletRequest,
        @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) userAgent: String,
        @RequestHeader(value = "Station-Id", required = false) stationId: String?,
        @RequestHeader(value = "Country", required = false) country: String?,
        @RequestHeader(value = HttpHeaders.CONTENT_TYPE) contentType: String?,
        @RequestHeader(value = "Station-Title", required = false) encStationTitle: String?,
        @RequestHeader(value = "Latitude", required = false) latitude: Double?,
        @RequestHeader(value = "Longitude", required = false) longitude: Double?,
        @RequestHeader(value = "Comment", required = false) encComment: String?,
        @RequestHeader(value = "Active", required = false) active: Boolean?,
        @AuthenticationPrincipal user: AuthUser
    ): ResponseEntity<InboxResponseDto> {
        val stationTitle = encStationTitle?.let { URLDecoder.decode(it, StandardCharsets.UTF_8) }
        val comment = encComment?.let { URLDecoder.decode(it, StandardCharsets.UTF_8) }
        val inboxResponse: InboxResponseDto = uploadPhoto(
            userAgent, request.inputStream, stationId,
            country, contentType, stationTitle, latitude, longitude, comment, active, user
        )
        return ResponseEntity<InboxResponseDto>(inboxResponse, toHttpStatus(inboxResponse.state))
    }

    private fun uploadPhoto(
        userAgent: String, body: InputStream?, stationId: String?,
        countryCode: String?, contentType: String?, stationTitle: String?,
        latitude: Double?, longitude: Double?, comment: String?,
        active: Boolean?, authUser: AuthUser
    ): InboxResponseDto {
        val locale: Locale = localeResolver.resolveLocale(requestUtil.request)
        val user: User = authUser.user
        if (user.locale != locale) {
            manageProfileUseCase.updateLocale(user, locale)
        }

        val inboxResponse: InboxResponse = manageInboxUseCase.uploadPhoto(
            clientInfo = userAgent,
            body = body,
            stationId = StringUtils.trimToNull(stationId),
            countryCode = StringUtils.trimToNull(countryCode),
            contentType = StringUtils.trimToEmpty(contentType).split(";")[0],
            stationTitle = stationTitle,
            latitude = latitude,
            longitude = longitude,
            comment = comment,
            active = active ?: true,
            user = user
        )
        return consumeBodyAndReturn(body, toDto(inboxResponse))
    }

    private fun consumeBodyAndReturn(body: InputStream?, response: InboxResponseDto): InboxResponseDto {
        body?.let {
            try {
                IOUtils.copy(it, NullOutputStream.INSTANCE)
            } catch (e: IOException) {
                log.warn("Unable to consume body", e)
            }
        }
        return response
    }

}

/**
 * jQuery.ajax treats anything different to 2xx as error, so we have to simplify the response codes
 */
private fun InboxResponseDto.State.toHttpStatusMultipartFormdata() = when (this) {
    InboxResponseDto.State.REVIEW, InboxResponseDto.State.CONFLICT
    -> HttpStatus.ACCEPTED

    InboxResponseDto.State.PHOTO_TOO_LARGE, InboxResponseDto.State.LAT_LON_OUT_OF_RANGE, InboxResponseDto.State.NOT_ENOUGH_DATA, InboxResponseDto.State.UNSUPPORTED_CONTENT_TYPE
    -> HttpStatus.OK

    else -> HttpStatus.INTERNAL_SERVER_ERROR
}

