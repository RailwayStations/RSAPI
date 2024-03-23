package org.railwaystations.rsapi.adapter.web.controller

import org.railwaystations.rsapi.utils.Logger
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ResponseStatusException
import java.security.Principal

@Controller
class AuthorizationConsentController(private val registeredClientRepository: RegisteredClientRepository) {

    private val log by Logger()

    @GetMapping(value = ["/oauth2/consent"])
    fun consent(
        principal: Principal, model: Model,
        @RequestParam(OAuth2ParameterNames.CLIENT_ID) clientId: String?,
        @RequestParam(OAuth2ParameterNames.STATE) state: String?
    ): String {
        val registeredClient = registeredClientRepository.findByClientId(clientId)
        if (registeredClient == null) {
            log.warn("Unknown clientId {}", clientId)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown clientId")
        }

        with(model) {
            addAttribute("clientId", clientId)
            addAttribute("clientName", registeredClient.clientName)
            addAttribute("state", state)
            addAttribute("principalName", principal.name)
        }

        return "consent"
    }
}
