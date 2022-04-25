package org.railwaystations.rsapi.adapter.in.web.controller;

import org.apache.commons.lang3.StringUtils;
import org.railwaystations.rsapi.adapter.in.web.model.ChangePasswordDto;
import org.railwaystations.rsapi.adapter.in.web.model.LicenseDto;
import org.railwaystations.rsapi.adapter.in.web.model.ProfileDto;
import org.railwaystations.rsapi.adapter.in.web.model.RegisterProfileDto;
import org.railwaystations.rsapi.adapter.in.web.model.UpdateProfileDto;
import org.railwaystations.rsapi.app.auth.AuthUser;
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

import static org.railwaystations.rsapi.adapter.in.web.model.LicenseDto.CC0_1_0_UNIVERSELL_CC0_1_0_;
import static org.railwaystations.rsapi.adapter.in.web.model.LicenseDto.CC_BY_SA_4_0;

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
                                                 @RequestBody(required = false) final ChangePasswordDto changePasswordDto) {
        manageProfileUseCase.changePassword(authUser.getUser(), changePasswordDto != null ? changePasswordDto.getNewPassword() : newPassword);
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
                                           @RequestBody @NotNull final RegisterProfileDto registerProfileDto) {
        manageProfileUseCase.register(toUser(registerProfileDto), userAgent);

        return ResponseEntity.accepted().build();
    }

    private User toUser(final RegisterProfileDto registerProfileDto) {
        return User.builder()
                .name(registerProfileDto.getNickname())
                .email(registerProfileDto.getEmail())
                .url(StringUtils.trimToEmpty(registerProfileDto.getLink()))
                .ownPhotos(registerProfileDto.getPhotoOwner())
                .anonymous(registerProfileDto.getAnonymous() != null && registerProfileDto.getAnonymous())
                .license(mapLicense(registerProfileDto.getLicense()))
                .sendNotifications(registerProfileDto.getSendNotifications() == null || registerProfileDto.getSendNotifications())
                .newPassword(registerProfileDto.getNewPassword())
                .build();
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
                .license(LicenseDto.fromValue(user.getLicense()))
                .link(user.getUrl())
                .nickname(user.getName())
                .photoOwner(user.isOwnPhotos());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, value = "/myProfile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> updateMyProfile(@RequestHeader(HttpHeaders.USER_AGENT) final String userAgent,
                                                  @RequestBody @NotNull final UpdateProfileDto updateProfileDto,
                                                  @AuthenticationPrincipal final AuthUser authUser) {
        manageProfileUseCase.updateProfile(authUser.getUser(), toUser(updateProfileDto), userAgent);

        return ResponseEntity.ok("Profile updated");
    }

    private User toUser(final UpdateProfileDto updateProfileDto) {
        return User.builder()
                .name(updateProfileDto.getNickname())
                .email(updateProfileDto.getEmail())
                .url(StringUtils.trimToEmpty(updateProfileDto.getLink()))
                .ownPhotos(updateProfileDto.getPhotoOwner())
                .anonymous(updateProfileDto.getAnonymous() != null && updateProfileDto.getAnonymous())
                .license(mapLicense(updateProfileDto.getLicense()))
                .sendNotifications(updateProfileDto.getSendNotifications() == null || updateProfileDto.getSendNotifications())
                .build();
    }

    private String mapLicense(final LicenseDto license) {
        return switch (license) {
            case CC0 -> CC0_1_0_UNIVERSELL_CC0_1_0_.getValue();
            case CC4 -> CC_BY_SA_4_0.getValue();
            default -> license.getValue();
        };
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
