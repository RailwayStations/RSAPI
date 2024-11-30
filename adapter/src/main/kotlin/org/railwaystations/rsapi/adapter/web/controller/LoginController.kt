package org.railwaystations.rsapi.adapter.web.controller

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.railwaystations.rsapi.adapter.web.RequestUtil
import org.railwaystations.rsapi.core.model.User
import org.railwaystations.rsapi.core.ports.inbound.ManageProfileUseCase
import org.railwaystations.rsapi.core.utils.Logger
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Controller
import org.springframework.validation.BindingResult
import org.springframework.validation.ObjectError
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.LocaleResolver


@Controller
class LoginController(
    private val manageProfileUseCase: ManageProfileUseCase,
    private val localeResolver: LocaleResolver,
    private val requestUtil: RequestUtil,
) {

    private val log by Logger()

    @GetMapping("/login")
    fun login(): String {
        return "login"
    }

    @PostMapping("/loginResetPassword")
    fun resetPassword(
        @RequestHeader(HttpHeaders.USER_AGENT) userAgent: String?,
        @RequestParam username: String
    ): String {
        try {
            manageProfileUseCase.resetPassword(username, userAgent)
        } catch (e: Exception) {
            log.error("Error resetting password", e)
            return "redirect:/login?reset_password_error&username=$username"
        }
        return "redirect:/login?reset_password_success&username=$username"
    }

    @Suppress("unused")
    @GetMapping("/loginRegister")
    fun register(@ModelAttribute newAccount: NewAccount): String {
        return "register"
    }

    @PostMapping("/loginRegister")
    fun registerNewAccount(
        @RequestHeader(HttpHeaders.USER_AGENT) userAgent: String?,
        @ModelAttribute @Valid newAccount: NewAccount,
        bindingResult: BindingResult
    ): String {
        if (!bindingResult.hasErrors()) {
            if (newAccount.password != newAccount.passwordRepeat) {
                bindingResult.rejectValue("passwordRepeat", "register.passwordsDontMatch")
            }
        }
        if (bindingResult.hasErrors()) {
            return "register"
        }

        try {
            val user = User(
                name = newAccount.username?.trim() ?: "",
                email = newAccount.email?.trim() ?: "",
                newPassword = newAccount.password,
                locale = localeResolver.resolveLocale(requestUtil.request),
            )
            manageProfileUseCase.register(user, userAgent)
        } catch (_: ManageProfileUseCase.ProfileConflictException) {
            log.warn("Register conflict with {}, '{}", newAccount.username, newAccount.email)
            bindingResult.reject("register.conflict")
            return "register"
        } catch (e: Exception) {
            log.error("Error Registering user {}", newAccount.username, e)
            bindingResult.addError(
                ObjectError(
                    "globalError",
                    arrayOf("register.error"),
                    null,
                    "Register conflict"
                )
            )
            return "register"
        }
        return "redirect:/login?register_success&username=" + newAccount.username
    }

    class NewAccount(
        @field:NotBlank
        @field:Size(max = 50)
        val username: String?,

        @field:NotBlank
        @field:Size(max = 100)
        @field:Email
        val email: String?,

        @field:NotBlank
        @field:Size(min = 8)
        val password: String?,

        @field:NotBlank
        val passwordRepeat: String?,
    )
}
