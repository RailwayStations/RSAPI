package org.railwaystations.rsapi.adapter.web.resources;

import org.railwaystations.rsapi.app.auth.AuthUser;
import org.railwaystations.rsapi.core.model.ChangePassword;
import org.railwaystations.rsapi.core.model.User;
import org.railwaystations.rsapi.core.services.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.constraints.NotNull;

@RestController
public class ProfileResource {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileResource.class);

    private final ProfileService profileService;

    public ProfileResource(final ProfileService profileService) {
        this.profileService = profileService;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE,value = "/changePassword")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> changePassword(@AuthenticationPrincipal final AuthUser authUser,
                                                 @RequestHeader(value = "New-Password", required = false) final String newPassword,
                                                 @RequestBody(required = false) final ChangePassword changePassword) {
        try {
            profileService.changePassword(authUser.getUser(), changePassword != null ? changePassword.getNewPassword() : newPassword);
        } catch (final IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>("Password changed", HttpStatus.OK);
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE,value = "/newUploadToken")
    public ResponseEntity<String> newUploadToken(@RequestHeader(HttpHeaders.USER_AGENT) final String userAgent,
                                                 @NotNull @RequestHeader("Email") final String email) {
        return resetPassword(userAgent, email);
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE,value = "/resetPassword")
    public ResponseEntity<String> resetPassword(@RequestHeader(HttpHeaders.USER_AGENT) final String userAgent,
                                                @NotNull @RequestHeader("NameOrEmail") final String nameOrEmail) {
        try {
            if (profileService.resetPassword(nameOrEmail, userAgent) == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (final IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE, value = "/registration")
    public ResponseEntity<String> register(@RequestHeader(HttpHeaders.USER_AGENT) final String userAgent,
                                           @RequestBody @NotNull final User newUser) {
        try {
            profileService.register(newUser, userAgent);
        } catch (final IllegalArgumentException e) {
            LOG.warn("Registration for '{}' with '{}' failed: {}", newUser.getName(), newUser.getEmail(), e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (final ProfileService.ProfileConflictException e) {
            return new ResponseEntity<>("Conflict with other user or email", HttpStatus.CONFLICT);
        }

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "/myProfile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<User> getMyProfile(@AuthenticationPrincipal final AuthUser authUser) {
        final User user = authUser.getUser();
        LOG.info("Get profile for '{}'", user.getEmail());
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, value = "/myProfile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> updateMyProfile(@RequestHeader(HttpHeaders.USER_AGENT) final String userAgent,
                                                  @RequestBody @NotNull final User newProfile,
                                                  @AuthenticationPrincipal final AuthUser authUser) {
        try {
            profileService.updateProfile(authUser.getUser(), newProfile, userAgent);
        } catch (final IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        } catch (final ProfileService.ProfileConflictException e) {
            return new ResponseEntity<>("Conflict with other user or email", HttpStatus.CONFLICT);
        }

        return new ResponseEntity<>("Profile updated", HttpStatus.OK);
    }

    @PostMapping("/resendEmailVerification")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> resendEmailVerification(@AuthenticationPrincipal final AuthUser authUser) {
        profileService.resendEmailVerification(authUser.getUser());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/emailVerification/{token}")
    public ResponseEntity<String> emailVerification(@PathVariable("token") final String token) {
        return profileService.emailVerification(token)
                .map(u -> new ResponseEntity<>("Email successfully verified!", HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

}
