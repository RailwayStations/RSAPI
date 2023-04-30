package org.railwaystations.rsapi.adapter.in.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthorizationConsentController {

    private final RegisteredClientRepository registeredClientRepository;

    @GetMapping(value = "/oauth2/consent")
    public String consent(Principal principal, Model model,
                          @RequestParam(OAuth2ParameterNames.CLIENT_ID) String clientId,
                          @RequestParam(OAuth2ParameterNames.STATE) String state) {

        var registeredClient = this.registeredClientRepository.findByClientId(clientId);
        if (registeredClient == null) {
            log.warn("Unknown clientId {}", clientId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown clientId");
        }

        model.addAttribute("clientId", clientId);
        model.addAttribute("clientName", registeredClient.getClientName());
        model.addAttribute("state", state);
        model.addAttribute("principalName", principal.getName());

        return "consent";
    }

}
