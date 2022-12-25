package org.railwaystations.rsapi.adapter.in.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/authentication/login")
    String login() {
        return "login";
    }

}
