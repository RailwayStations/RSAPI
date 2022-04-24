package org.railwaystations.rsapi.adapter.in.web.controller;

import org.railwaystations.rsapi.adapter.in.web.model.ProfileDto;
import org.railwaystations.rsapi.app.auth.AuthUser;
import org.railwaystations.rsapi.core.model.PasswordChangeCommand;
import org.railwaystations.rsapi.core.model.User;
import org.railwaystations.rsapi.core.ports.in.ManageProfileUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;

@RestController
public class ProfileController {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileController.class);

    private final ManageProfileUseCase manageProfileUseCase;

    public ProfileController(final ManageProfileUseCase manageProfileUseCase) {
        this.manageProfileUseCase = manageProfileUseCase;
    }

    @PostMapping("/changePassword")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> changePassword(@AuthenticationPrincipal final AuthUser authUser,
                                                 @RequestHeader(value = "New-Password", required = false) final String newPassword,
                                                 @RequestBody(required = false) final PasswordChangeCommand passwordChangeCommand) {
        manageProfileUseCase.changePassword(authUser.getUser(), passwordChangeCommand != null ? passwordChangeCommand : new PasswordChangeCommand(newPassword));
        return ResponseEntity.ok("Password changed");
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE,value = "/newUploadToken")
    public ResponseEntity<String> newUploadToken(@RequestHeader(HttpHeaders.USER_AGENT) final String userAgent,
                                                 @NotNull @RequestHeader("Email") final String email) {
        return resetPassword(userAgent, email);
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE,value = "/resetPassword")
    public ResponseEntity<String> resetPassword(@RequestHeader(HttpHeaders.USER_AGENT) final String userAgent,
                                                @NotNull @RequestHeader("NameOrEmail") final String nameOrEmail) {
        if (manageProfileUseCase.resetPassword(nameOrEmail, userAgent) == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.accepted().build();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, value = "/registration")
    public ResponseEntity<String> register(@RequestHeader(HttpHeaders.USER_AGENT) final String userAgent,
                                           @RequestBody @NotNull final User newUser) {
        manageProfileUseCase.register(newUser, userAgent);

        return ResponseEntity.accepted().build();
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "/myProfile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileDto> getMyProfile(@AuthenticationPrincipal final AuthUser authUser) {
        final User user = authUser.getUser();
        LOG.info("Get profile for '{}'", user.getEmail());
        return ResponseEntity.ok(toProfileDto(user));
    }

    private ProfileDto toProfileDto(final User user) {
        return new ProfileDto()
                .admin(user.isAdmin())
                .email(user.getEmail())
                .anonymous(user.isAnonymous())
                .emailVerified(user.isEmailVerified())
                .sendNotifications(user.isSendNotifications())
                .license(ProfileDto.LicenseEnum.fromValue(user.getLicense()))
                .link(user.getUrl())
                .nickname(user.getName())
                .photoOwner(user.isOwnPhotos());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, value = "/myProfile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> updateMyProfile(@RequestHeader(HttpHeaders.USER_AGENT) final String userAgent,
                                                  @RequestBody @NotNull final User newProfile,
                                                  @AuthenticationPrincipal final AuthUser authUser) {
        manageProfileUseCase.updateProfile(authUser.getUser(), newProfile, userAgent);

        return ResponseEntity.ok("Profile updated");
    }

    @PostMapping("/resendEmailVerification")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> resendEmailVerification(@AuthenticationPrincipal final AuthUser authUser) {
        manageProfileUseCase.resendEmailVerification(authUser.getUser());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/emailVerification/{token}")
    public ResponseEntity<String> emailVerification(@PathVariable("token") final String token) {
        return manageProfileUseCase.emailVerification(token)
                .map(u -> new ResponseEntity<>("Email successfully verified!", HttpStatus.OK))
                .orElse(ResponseEntity.notFound().build());
    }

}
