package org.railwaystations.rsapi.adapter.web.controller

import org.railwaystations.rsapi.adapter.web.RequestUtil
import org.railwaystations.rsapi.adapter.web.api.ProfileApi
import org.railwaystations.rsapi.adapter.web.model.ChangePasswordDto
import org.railwaystations.rsapi.adapter.web.model.LicenseDto
import org.railwaystations.rsapi.adapter.web.model.ProfileDto
import org.railwaystations.rsapi.adapter.web.model.UpdateProfileDto
import org.railwaystations.rsapi.core.model.License
import org.railwaystations.rsapi.core.model.User
import org.railwaystations.rsapi.core.ports.inbound.ManageProfileUseCase
import org.railwaystations.rsapi.core.utils.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.LocaleResolver
import java.net.URI
import java.util.*

@RestController
class ProfileController(
    private val manageProfileUseCase: ManageProfileUseCase,
    private val localeResolver: LocaleResolver,
    private val requestUtil: RequestUtil,
) : ProfileApi {

    private val log by Logger()

    @PreAuthorize("isAuthenticated()")
    override fun postChangePassword(authorization: String, changePasswordDto: ChangePasswordDto): ResponseEntity<Unit> {
        manageProfileUseCase.changePassword(requestUtil.authUser.user, changePasswordDto.newPassword)
        return ResponseEntity.ok().build()
    }

    override fun getEmailVerification(token: String): ResponseEntity<String> {
        return manageProfileUseCase.emailVerification(token)
            ?.let { _ ->
                ResponseEntity(
                    "Email successfully verified!",
                    HttpStatus.OK
                )
            }
            ?: ResponseEntity.notFound().build()
    }

    @PreAuthorize("isAuthenticated()")
    override fun deleteMyProfile(authorization: String): ResponseEntity<Unit> {
        manageProfileUseCase.deleteProfile(requestUtil.authUser.user, requestUtil.userAgent)
        return ResponseEntity.noContent().build()
    }

    @PreAuthorize("isAuthenticated()")
    override fun getMyProfile(authorization: String): ResponseEntity<ProfileDto> {
        val user = requestUtil.authUser.user
        log.info("Get profile for '{}'", user.email)
        return ResponseEntity.ok(user.toDto())
    }

    @PreAuthorize("isAuthenticated()")
    override fun postMyProfile(authorization: String, profile: UpdateProfileDto): ResponseEntity<Unit> {
        val locale = localeResolver.resolveLocale(requestUtil.request)
        log.info("User locale {}", locale)
        manageProfileUseCase.updateProfile(requestUtil.authUser.user, profile.toDomain(locale), requestUtil.userAgent)
        return ResponseEntity.ok().build()
    }

    @PreAuthorize("isAuthenticated()")
    override fun postResendEmailVerification(authorization: String): ResponseEntity<Unit> {
        manageProfileUseCase.resendEmailVerification(requestUtil.authUser.user)
        return ResponseEntity.ok().build()
    }

}

private fun UpdateProfileDto.toDomain(locale: Locale) = User(
    name = nickname,
    email = email,
    url = link?.toString(),
    ownPhotos = photoOwner ?: false,
    anonymous = anonymous ?: false,
    license = license.toDomain(),
    sendNotifications = sendNotifications ?: true,
    locale = locale,
)

private fun User.toDto() = ProfileDto(
    nickname = name,
    license = license.toDto(),
    photoOwner = ownPhotos,
    email = email,
    link = url?.let { URI.create(it) },
    anonymous = anonymous,
    admin = admin,
    emailVerified = isEmailVerified,
    sendNotifications = sendNotifications,
)

private fun License?.toDto() = when (this) {
    License.CC0_10
    -> LicenseDto.CC0_1_PERIOD0_UNIVERSELL_LEFT_PARENTHESIS_CC0_1_PERIOD0_RIGHT_PARENTHESIS

    License.CC_BY_SA_40
    -> LicenseDto.CC_BY_MINUS_SA_4_PERIOD0

    else -> LicenseDto.UNKNOWN
}

fun LicenseDto?.toDomain() = when (this) {
    LicenseDto.CC0_1_PERIOD0_UNIVERSELL_LEFT_PARENTHESIS_CC0_1_PERIOD0_RIGHT_PARENTHESIS, LicenseDto.CC0
    -> License.CC0_10

    LicenseDto.CC_BY_MINUS_SA_4_PERIOD0, LicenseDto.CC4
    -> License.CC_BY_SA_40

    LicenseDto.UNKNOWN, null -> License.UNKNOWN
}

