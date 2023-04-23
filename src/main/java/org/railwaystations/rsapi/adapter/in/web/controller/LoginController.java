package org.railwaystations.rsapi.adapter.in.web.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.railwaystations.rsapi.core.model.User;
import org.railwaystations.rsapi.core.ports.in.ManageProfileUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Objects;

@Controller
@Slf4j
public class LoginController {

    @Autowired
    private ManageProfileUseCase manageProfileUseCase;

    @GetMapping("/login")
    String login() {
        return "login";
    }

    @PostMapping("/loginResetPassword")
    String resetPassword(@RequestHeader(HttpHeaders.USER_AGENT) String userAgent, @RequestParam String username) {
        try {
            manageProfileUseCase.resetPassword(username, userAgent);
        } catch (Exception e) {
            return "redirect:/login?reset_password_error&username=" + username;
        }
        return "redirect:/login?reset_password_success&username=" + username;
    }

    @GetMapping("/loginRegister")
    String register(@ModelAttribute NewAccount newAccount) {
        return "register";
    }

    @PostMapping("/loginRegister")
    String registerNewAccount(@RequestHeader(HttpHeaders.USER_AGENT) String userAgent, @ModelAttribute @Valid NewAccount newAccount, BindingResult bindingResult) {

        if (!bindingResult.hasErrors()) {
            if (!Objects.equals(newAccount.password, newAccount.passwordRepeat)) {
                bindingResult.rejectValue("passwordRepeat", "register.passwordsDontMatch");
            }
        }
        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            var user = User.builder()
                    .name(StringUtils.trimToEmpty(newAccount.username))
                    .email(StringUtils.trimToEmpty(newAccount.email))
                    .newPassword(newAccount.password)
                    .build();
            manageProfileUseCase.register(user, userAgent);
        } catch (ManageProfileUseCase.ProfileConflictException e) {
            log.warn("Register conflict with {}, '{}", newAccount.username, newAccount.email);
            bindingResult.reject("register.conflict");
            return "register";
        } catch (Exception e) {
            log.error("Error Registering user {}", newAccount.username, e);
            bindingResult.addError(new ObjectError("globalError", new String[]{"register.error"}, null, "Register conflict"));
            return "register";
        }
        return "redirect:/login?register_success&username=" + newAccount.username;
    }

    public record NewAccount(@NotBlank @Size(max = 50) String username,
                             @NotBlank @Size(max = 100) @Email String email,
                             @NotNull @Size(min = 8) String password,
                             String passwordRepeat) {
    }

}
