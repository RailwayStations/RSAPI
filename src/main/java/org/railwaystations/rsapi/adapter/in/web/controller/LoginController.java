package org.railwaystations.rsapi.adapter.in.web.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.railwaystations.rsapi.core.model.User;
import org.railwaystations.rsapi.core.ports.in.ManageProfileUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Objects;

@Controller
public class LoginController {

    @Autowired
    private ManageProfileUseCase manageProfileUseCase;

    @Autowired
    private MessageSource messageSource;

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
                var message = messageSource.getMessage("PasswordsDontMatch", null, LocaleContextHolder.getLocale());
                bindingResult.addError(new FieldError("newAccount", "passwordRepeat", newAccount.passwordRepeat, false, null, null, message));
            }
        }
        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            // TODO: map to User
            User user = null;
            manageProfileUseCase.register(user, userAgent);
        } catch (Exception e) {
            // TODO: map errors
            return "redirect:/login?username=" + newAccount.username;
        }
        return "redirect:/login?username=" + newAccount.username;
    }

    public record NewAccount(@NotBlank String username,
                             @NotBlank @Email String email,
                             @NotNull @Size(min = 8) String password,
                             @NotBlank String passwordRepeat) {
    }

}
