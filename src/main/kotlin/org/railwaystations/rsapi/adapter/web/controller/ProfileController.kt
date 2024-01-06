package org.railwaystations.rsapi.adapter.web.controller

import jakarta.validation.Valid
import org.railwaystations.rsapi.adapter.web.RequestUtil
import org.railwaystations.rsapi.adapter.web.model.ChangePasswordDto
import org.railwaystations.rsapi.adapter.web.model.LicenseDto
import org.railwaystations.rsapi.adapter.web.model.ProfileDto
import org.railwaystations.rsapi.adapter.web.model.UpdateProfileDto
import org.railwaystations.rsapi.core.model.License
import org.railwaystations.rsapi.core.model.User
import org.railwaystations.rsapi.core.ports.ManageProfileUseCase
import org.railwaystations.rsapi.utils.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.LocaleResolver
import java.net.URI
import java.util.*

@RestController
class ProfileController(
    private val manageProfileUseCase: ManageProfileUseCase,
    private val localeResolver: LocaleResolver,
    private val requestUtil: RequestUtil,
) {

    private val log by Logger()

    private fun toProfileDto(user: User): ProfileDto {
        return ProfileDto(
            nickname = user.name, license = toLicenseDto(user.license), photoOwner = user.ownPhotos,
            email = user.email,
            link = user.url?.let { URI.create(it) },
            anonymous = user.anonymous,
            admin = user.admin,
            emailVerified = user.isEmailVerified,
            sendNotifications = user.sendNotifications,
        )
    }

    private fun toLicenseDto(license: License?): LicenseDto {
        return when (license) {
            License.CC0_10 -> LicenseDto.CC0_1_PERIOD0_UNIVERSELL_LEFT_PARENTHESIS_CC0_1_PERIOD0_RIGHT_PARENTHESIS
            License.CC_BY_SA_40 -> LicenseDto.CC_BY_MINUS_SA_4_PERIOD0
            else -> LicenseDto.UNKNOWN
        }
    }

    private fun toUser(updateProfileDto: UpdateProfileDto): User {
        val locale: Locale = localeResolver.resolveLocale(requestUtil.request)
        log.info("User locale {}", locale)
        return User(
            name = updateProfileDto.nickname,
            email = updateProfileDto.email,
            url = updateProfileDto.link?.toString(),
            ownPhotos = updateProfileDto.photoOwner,
            anonymous = updateProfileDto.anonymous ?: false,
            license = toLicense(updateProfileDto.license),
            sendNotifications = updateProfileDto.sendNotifications ?: true,
            locale = locale,
        )
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(
        method = [RequestMethod.POST],
        value = ["/changePassword"],
        produces = ["text/plain"],
        consumes = ["application/json"]
    )
    fun changePasswordPost(
        @RequestHeader(required = true, value = "Authorization") authorization: String,
        @Valid @RequestBody changePasswordDto: ChangePasswordDto
    ): ResponseEntity<Unit> {
        manageProfileUseCase.changePassword(requestUtil.authUser.user, changePasswordDto.newPassword)
        return ResponseEntity.ok().build()
    }

    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/emailVerification/{token}"],
        produces = ["text/plain"]
    )
    fun emailVerificationTokenGet(@PathVariable("token") token: String): ResponseEntity<String> {
        return manageProfileUseCase.emailVerification(token)
            ?.let { _: User ->
                ResponseEntity(
                    "Email successfully verified!",
                    HttpStatus.OK
                )
            }
            ?: ResponseEntity.notFound().build()
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(
        method = [RequestMethod.DELETE],
        value = ["/myProfile"]
    )
    fun myProfileDelete(
        @RequestHeader(
            required = true,
            value = "Authorization"
        ) authorization: String
    ): ResponseEntity<Unit> {
        manageProfileUseCase.deleteProfile(requestUtil.authUser.user, requestUtil.userAgent)
        return ResponseEntity.noContent().build()
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/myProfile"],
        produces = ["application/json"]
    )
    fun myProfileGet(
        @RequestHeader(
            required = true,
            value = "Authorization"
        ) authorization: String
    ): ResponseEntity<ProfileDto> {
        val user = requestUtil.authUser.user
        log.info("Get profile for '{}'", user.email)
        return ResponseEntity.ok(toProfileDto(user))
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(
        method = [RequestMethod.POST],
        value = ["/myProfile"],
        produces = ["text/plain"],
        consumes = ["application/json"]
    )
    fun myProfilePost(
        @RequestHeader(required = true, value = "Authorization") authorization: String,
        @Valid @RequestBody profile: UpdateProfileDto
    ): ResponseEntity<Unit> {
        manageProfileUseCase.updateProfile(requestUtil.authUser.user, toUser(profile), requestUtil.userAgent)
        return ResponseEntity.ok().build()
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(
        method = [RequestMethod.POST],
        value = ["/resendEmailVerification"]
    )
    fun resendEmailVerificationPost(
        @RequestHeader(
            required = true,
            value = "Authorization"
        ) authorization: String
    ): ResponseEntity<Unit> {
        manageProfileUseCase.resendEmailVerification(requestUtil.authUser.user)
        return ResponseEntity.ok().build()
    }

    companion object {
        @JvmStatic
        fun toLicense(license: LicenseDto?): License {
            return when (license) {
                LicenseDto.CC0_1_PERIOD0_UNIVERSELL_LEFT_PARENTHESIS_CC0_1_PERIOD0_RIGHT_PARENTHESIS, LicenseDto.CC0 -> License.CC0_10
                LicenseDto.CC_BY_MINUS_SA_4_PERIOD0, LicenseDto.CC4 -> License.CC_BY_SA_40
                LicenseDto.UNKNOWN, null -> License.UNKNOWN
            }
        }
    }
}
