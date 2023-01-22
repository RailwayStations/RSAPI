package org.railwaystations.rsapi.adapter.in.web.controller;

import org.railwaystations.rsapi.core.ports.in.ManageProfileUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class LoginController {

    @Autowired
    private ManageProfileUseCase manageProfileUseCase;

    @GetMapping("/login")
    String login() {
        return "login";
    }

    @PostMapping("/loginResetPassword")
    ModelAndView resetPassword(@RequestHeader(HttpHeaders.USER_AGENT) String userAgent, @RequestParam String username) {
        try {
            manageProfileUseCase.resetPassword(username, userAgent);
        } catch (Exception e) {
            return new ModelAndView("redirect:/login?reset_password_error&username=" + username);
        }
        return new ModelAndView("redirect:/login?reset_password_success&username=" + username);
    }

}
