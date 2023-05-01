package org.railwaystations.rsapi.adapter.in.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.railwaystations.rsapi.adapter.in.web.api.ProfileApi;
import org.railwaystations.rsapi.adapter.in.web.model.ChangePasswordDto;
import org.railwaystations.rsapi.adapter.in.web.model.LicenseDto;
import org.railwaystations.rsapi.adapter.in.web.model.ProfileDto;
import org.railwaystations.rsapi.adapter.in.web.model.RegisterProfileDto;
import org.railwaystations.rsapi.adapter.in.web.model.UpdateProfileDto;
import org.railwaystations.rsapi.core.model.License;
import org.railwaystations.rsapi.core.model.User;
import org.railwaystations.rsapi.core.ports.in.ManageProfileUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.LocaleResolver;

import java.net.URI;
import java.util.Locale;

import static org.railwaystations.rsapi.adapter.in.web.RequestUtil.getAuthUser;
import static org.railwaystations.rsapi.adapter.in.web.RequestUtil.getRequest;
import static org.railwaystations.rsapi.adapter.in.web.RequestUtil.getUserAgent;

@RestController
@Slf4j
@RequiredArgsConstructor
public class ProfileController implements ProfileApi {

    private final ManageProfileUseCase manageProfileUseCase;

    private final LocaleResolver localeResolver;

    private User toUser(RegisterProfileDto registerProfileDto) {
        return User.builder()
                .name(registerProfileDto.getNickname())
                .email(registerProfileDto.getEmail())
                .url(registerProfileDto.getLink() != null ? registerProfileDto.getLink().toString() : null)
                .ownPhotos(registerProfileDto.getPhotoOwner())
                .anonymous(registerProfileDto.getAnonymous() != null && registerProfileDto.getAnonymous())
                .license(toLicense(registerProfileDto.getLicense()))
                .sendNotifications(registerProfileDto.getSendNotifications() == null || registerProfileDto.getSendNotifications())
                .newPassword(registerProfileDto.getNewPassword())
                .locale(localeResolver.resolveLocale(getRequest()))
                .build();
    }

    private License toLicense(LicenseDto license) {
        if (license == null) {
            return License.UNKNOWN;
        }
        return switch (license) {
            case CC0_1_0_UNIVERSELL_CC0_1_0_, CC0 -> License.CC0_10;
            case CC_BY_SA_4_0, CC4 -> License.CC_BY_SA_40;
            case UNKNOWN -> License.UNKNOWN;
        };
    }

    private ProfileDto toProfileDto(User user) {
        return new ProfileDto(user.getName(), toLicenseDto(user.getLicense()), user.isOwnPhotos())
                .admin(user.isAdmin())
                .email(user.getEmail())
                .anonymous(user.isAnonymous())
                .emailVerified(user.isEmailVerified())
                .sendNotifications(user.isSendNotifications())
                .link(user.getUrl() != null ? URI.create(user.getUrl()) : null);
    }

    private LicenseDto toLicenseDto(License license) {
        if (license == null) {
            return LicenseDto.UNKNOWN;
        }
        return switch (license) {
            case CC0_10 -> LicenseDto.CC0_1_0_UNIVERSELL_CC0_1_0_;
            case CC_BY_SA_40 -> LicenseDto.CC_BY_SA_4_0;
            default -> LicenseDto.UNKNOWN;
        };
    }

    private User toUser(UpdateProfileDto updateProfileDto) {
        Locale locale = localeResolver.resolveLocale(getRequest());
        log.info("User locale {}", locale);
        return User.builder()
                .name(updateProfileDto.getNickname())
                .email(updateProfileDto.getEmail())
                .url(updateProfileDto.getLink() != null ? updateProfileDto.getLink().toString() : null)
                .ownPhotos(updateProfileDto.getPhotoOwner())
                .anonymous(updateProfileDto.getAnonymous() != null && updateProfileDto.getAnonymous())
                .license(toLicense(updateProfileDto.getLicense()))
                .sendNotifications(updateProfileDto.getSendNotifications() == null || updateProfileDto.getSendNotifications())
                .locale(locale)
                .build();
    }

    @PreAuthorize("isAuthenticated()")
    @Override
    public ResponseEntity<Void> changePasswordPost(String newPassword, ChangePasswordDto changePasswordDto) {
        manageProfileUseCase.changePassword(getAuthUser().getUser(), changePasswordDto != null ? changePasswordDto.getNewPassword() : newPassword);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<String> emailVerificationTokenGet(String token) {
        return manageProfileUseCase.emailVerification(token)
                .map(u -> new ResponseEntity<>("Email successfully verified!", HttpStatus.OK))
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("isAuthenticated()")
    @Override
    public ResponseEntity<Void> myProfileDelete() {
        manageProfileUseCase.deleteProfile(getAuthUser().getUser(), getUserAgent());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("isAuthenticated()")
    @Override
    public ResponseEntity<ProfileDto> myProfileGet() {
        User user = getAuthUser().getUser();
        log.info("Get profile for '{}'", user.getEmail());
        return ResponseEntity.ok(toProfileDto(user));
    }

    @PreAuthorize("isAuthenticated()")
    @Override
    public ResponseEntity<Void> myProfilePost(UpdateProfileDto profile) {
        manageProfileUseCase.updateProfile(getAuthUser().getUser(), toUser(profile), getUserAgent());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> registrationPost(RegisterProfileDto registration) {
        manageProfileUseCase.register(toUser(registration), getUserAgent());
        return ResponseEntity.accepted().build();
    }

    @PreAuthorize("isAuthenticated()")
    @Override
    public ResponseEntity<Void> resendEmailVerificationPost() {
        manageProfileUseCase.resendEmailVerification(getAuthUser().getUser());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> resetPasswordPost(String nameOrEmail) {
        manageProfileUseCase.resetPassword(nameOrEmail, getUserAgent());
        return ResponseEntity.accepted().build();
    }

}
