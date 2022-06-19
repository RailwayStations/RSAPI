package org.railwaystations.rsapi.adapter.in.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.railwaystations.rsapi.adapter.in.web.model.ChangePasswordDto;
import org.railwaystations.rsapi.adapter.in.web.model.LicenseDto;
import org.railwaystations.rsapi.adapter.in.web.model.ProfileDto;
import org.railwaystations.rsapi.adapter.in.web.model.RegisterProfileDto;
import org.railwaystations.rsapi.adapter.in.web.model.UpdateProfileDto;
import org.railwaystations.rsapi.app.auth.AuthUser;
import org.railwaystations.rsapi.core.model.License;
import org.railwaystations.rsapi.core.model.User;
import org.railwaystations.rsapi.core.ports.in.ManageProfileUseCase;
import org.springframework.beans.factory.annotation.Autowired;
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
@Slf4j
public class ProfileController {

    @Autowired
    private ManageProfileUseCase manageProfileUseCase;

    @PostMapping("/changePassword")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> changePassword(@AuthenticationPrincipal AuthUser authUser,
                                                 @RequestHeader(value = "New-Password", required = false) String newPassword,
                                                 @RequestBody(required = false) ChangePasswordDto changePasswordDto) {
        manageProfileUseCase.changePassword(authUser.getUser(), changePasswordDto != null ? changePasswordDto.getNewPassword() : newPassword);
        return ResponseEntity.ok("Password changed");
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE,value = "/newUploadToken")
    public ResponseEntity<String> newUploadToken(@RequestHeader(HttpHeaders.USER_AGENT) String userAgent,
                                                 @NotNull @RequestHeader("Email") String email) {
        return resetPassword(userAgent, email);
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE,value = "/resetPassword")
    public ResponseEntity<String> resetPassword(@RequestHeader(HttpHeaders.USER_AGENT) String userAgent,
                                                @NotNull @RequestHeader("NameOrEmail") String nameOrEmail) {
        manageProfileUseCase.resetPassword(nameOrEmail, userAgent);
        return ResponseEntity.accepted().build();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, value = "/registration")
    public ResponseEntity<String> register(@RequestHeader(HttpHeaders.USER_AGENT) String userAgent,
                                           @RequestBody @NotNull RegisterProfileDto registerProfileDto) {
        manageProfileUseCase.register(toUser(registerProfileDto), userAgent);

        return ResponseEntity.accepted().build();
    }

    private User toUser(RegisterProfileDto registerProfileDto) {
        return User.builder()
                .name(registerProfileDto.getNickname())
                .email(registerProfileDto.getEmail())
                .url(StringUtils.trimToEmpty(registerProfileDto.getLink()))
                .ownPhotos(registerProfileDto.getPhotoOwner())
                .anonymous(registerProfileDto.getAnonymous() != null && registerProfileDto.getAnonymous())
                .license(toLicense(registerProfileDto.getLicense()))
                .sendNotifications(registerProfileDto.getSendNotifications() == null || registerProfileDto.getSendNotifications())
                .newPassword(registerProfileDto.getNewPassword())
                .build();
    }

    private License toLicense(LicenseDto license) {
        if (license == null) {
            return null;
        }
        return switch (license) {
            case CC0_1_0_UNIVERSELL_CC0_1_0_, CC0 -> License.CC0_10;
            case CC_BY_SA_4_0, CC4 -> License.CC_BY_SA_40;
        };
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "/myProfile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileDto> getMyProfile(@AuthenticationPrincipal AuthUser authUser) {
        User user = authUser.getUser();
        log.info("Get profile for '{}'", user.getEmail());
        return ResponseEntity.ok(toProfileDto(user));
    }

    private ProfileDto toProfileDto(User user) {
        return new ProfileDto()
                .admin(user.isAdmin())
                .email(user.getEmail())
                .anonymous(user.isAnonymous())
                .emailVerified(user.isEmailVerified())
                .sendNotifications(user.isSendNotifications())
                .license(toLicenseDto(user.getLicense()))
                .link(user.getUrl())
                .nickname(user.getName())
                .photoOwner(user.isOwnPhotos());
    }

    private LicenseDto toLicenseDto(License license) {
        if (license == null) {
            return null;
        }
        return switch (license) {
            case CC0_10 -> LicenseDto.CC0_1_0_UNIVERSELL_CC0_1_0_;
            case CC_BY_SA_40 -> LicenseDto.CC_BY_SA_4_0;
            default -> null;
        };
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, value = "/myProfile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> updateMyProfile(@RequestHeader(HttpHeaders.USER_AGENT) String userAgent,
                                                  @RequestBody @NotNull UpdateProfileDto updateProfileDto,
                                                  @AuthenticationPrincipal AuthUser authUser) {
        manageProfileUseCase.updateProfile(authUser.getUser(), toUser(updateProfileDto), userAgent);

        return ResponseEntity.ok("Profile updated");
    }

    private User toUser(UpdateProfileDto updateProfileDto) {
        return User.builder()
                .name(updateProfileDto.getNickname())
                .email(updateProfileDto.getEmail())
                .url(StringUtils.trimToEmpty(updateProfileDto.getLink()))
                .ownPhotos(updateProfileDto.getPhotoOwner())
                .anonymous(updateProfileDto.getAnonymous() != null && updateProfileDto.getAnonymous())
                .license(toLicense(updateProfileDto.getLicense()))
                .sendNotifications(updateProfileDto.getSendNotifications() == null || updateProfileDto.getSendNotifications())
                .build();
    }

    @PostMapping("/resendEmailVerification")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> resendEmailVerification(@AuthenticationPrincipal AuthUser authUser) {
        manageProfileUseCase.resendEmailVerification(authUser.getUser());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/emailVerification/{token}")
    public ResponseEntity<String> emailVerification(@PathVariable("token") String token) {
        return manageProfileUseCase.emailVerification(token)
                .map(u -> new ResponseEntity<>("Email successfully verified!", HttpStatus.OK))
                .orElse(ResponseEntity.notFound().build());
    }

}
