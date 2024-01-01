package org.railwaystations.rsapi.core.services;

import org.junit.jupiter.api.Test;
import org.railwaystations.rsapi.adapter.out.db.OAuth2AuthorizationDao;
import org.railwaystations.rsapi.adapter.out.db.UserDao;
import org.railwaystations.rsapi.adapter.out.monitoring.MockMonitor;
import org.railwaystations.rsapi.app.auth.LazySodiumPasswordEncoder;
import org.railwaystations.rsapi.app.config.MessageSourceConfig;
import org.railwaystations.rsapi.core.model.License;
import org.railwaystations.rsapi.core.model.User;
import org.railwaystations.rsapi.core.ports.in.ManageProfileUseCase;
import org.railwaystations.rsapi.core.ports.out.Mailer;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.railwaystations.rsapi.adapter.in.web.controller.ProfileControllerTest.EXISTING_USER_ID;
import static org.railwaystations.rsapi.adapter.in.web.controller.ProfileControllerTest.USER_EMAIL;
import static org.railwaystations.rsapi.adapter.in.web.controller.ProfileControllerTest.USER_NAME;

class ProfileServiceTest {

    public static final String USER_AGENT = "UserAgent";

    private final UserDao userDao = mock(UserDao.class);

    private final MockMonitor monitor = new MockMonitor();

    private final Mailer mailer = mock(Mailer.class);

    private final OAuth2AuthorizationDao authorizationDao = mock(OAuth2AuthorizationDao.class);

    private final ProfileService sut = new ProfileService(monitor, mailer, userDao, authorizationDao, "EMAIL_VERIFICATION_URL", new LazySodiumPasswordEncoder(), new MessageSourceConfig().messageSource());

    @Test
    void testRegisterInvalidData() {
        var newUser = User.builder()
                .name("nickname")
                .ownPhotos(true)
                .build();
        assertThatThrownBy(() -> sut.register(newUser, USER_AGENT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registerNewUser() {
        var newUser = createNewUser().build();

        sut.register(newUser, USER_AGENT);

        verify(userDao).findByNormalizedName(USER_NAME);
        verify(userDao).countBlockedUsername(USER_NAME);
        verify(userDao).findByEmail(USER_EMAIL);
        verify(userDao).insert(any(User.class), anyString(), anyString());
        verify(userDao, never()).updateCredentials(anyInt(), anyString());

        assertThat(monitor.getMessages().getFirst()).isEqualTo("New registration{nickname='%s', email='%s'}\nvia %s".formatted(USER_NAME, USER_EMAIL, USER_AGENT));
        assertNewPasswordEmail();

        verifyNoMoreInteractions(userDao);
    }


    public static void assertVerificationEmail(Mailer mailer) {
        verify(mailer, times(1))
                .send(anyString(),
                        anyString(), matches("""
                                Hello,

                                please click on EMAIL_VERIFICATION_URL.* to verify your eMail-Address.

                                Cheers
                                Your Railway-Stations-Team

                                ---
                                Hallo,

                                bitte klicke auf EMAIL_VERIFICATION_URL.*, um Deine eMail-Adresse zu verifizieren.

                                Viele Grüße
                                Dein Bahnhofsfoto-Team"""));
    }

    @Test
    void registerNewUserWithPassword() {
        var newUser = createNewUser()
                .newPassword("verySecretPassword")
                .build();

        sut.register(newUser, USER_AGENT);

        verify(userDao).findByNormalizedName(USER_NAME);
        verify(userDao).countBlockedUsername(USER_NAME);
        verify(userDao).findByEmail(USER_EMAIL);
        verify(userDao).insert(any(User.class), anyString(), anyString());
        verify(userDao, never()).updateCredentials(anyInt(), anyString());

        assertThat(monitor.getMessages().getFirst()).isEqualTo("New registration{nickname='%s', email='%s'}\nvia %s".formatted(USER_NAME, USER_EMAIL, USER_AGENT));
        assertVerificationEmail(mailer);

        verifyNoMoreInteractions(userDao);
    }

    @Test
    void updateMyProfileNewMail() {
        when(userDao.findByEmail("newname@example.com")).thenReturn(Optional.empty());
        var existingUser = givenExistingUser();
        var updatedUser = createNewUser().email("newname@example.com").build();

        sut.updateProfile(existingUser, updatedUser, USER_AGENT);

        assertVerificationEmail(mailer);
        var user = User.builder()
                .id(EXISTING_USER_ID)
                .name(USER_NAME)
                .license(License.CC0_10)
                .email("newname@example.com")
                .ownPhotos(true)
                .anonymous(false)
                .url("http://twitter.com/")
                .sendNotifications(true).build();
        verify(userDao).update(user.getId(), user);
    }

    @Test
    void changePasswordTooShortBody() {
        var user = givenExistingUser();

        assertThatThrownBy(() -> sut.changePassword(user, "secret"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changePasswordBody() {
        var user = givenExistingUser();

        sut.changePassword(user, "secretlong");

        verify(userDao).updateCredentials(eq(user.getId()), anyString());
        verify(authorizationDao).deleteAllByUser(user.getName());
    }

    @Test
    void verifyEmailSuccess() {
        var token = "verification";
        var user = User.builder()
                .id(EXISTING_USER_ID)
                .name(USER_NAME)
                .license(License.CC0_10)
                .email(USER_EMAIL)
                .ownPhotos(true)
                .anonymous(false)
                .url("https://link@example.com")
                .emailVerification(token)
                .sendNotifications(true).build();
        when(userDao.findByEmailVerification(token)).thenReturn(Optional.of(user));

        sut.emailVerification(token);

        assertThat(monitor.getMessages().getFirst()).isEqualTo(String.format("Email verified {nickname='%s', email='%s'}", USER_NAME, USER_EMAIL));
        verify(userDao).updateEmailVerification(EXISTING_USER_ID, User.EMAIL_VERIFIED);
    }

    @Test
    void verifyEmailFailed() {
        var token = "verification";
        var user = User.builder()
                .id(EXISTING_USER_ID)
                .name(USER_NAME)
                .license(License.CC0_10)
                .email(USER_EMAIL)
                .ownPhotos(true)
                .anonymous(false)
                .url("https://link@example.com")
                .emailVerification(token)
                .sendNotifications(true).build();
        when(userDao.findByEmailVerification(token)).thenReturn(Optional.of(user));

        sut.emailVerification("wrong_token");

        assertThat(monitor.getMessages().isEmpty()).isTrue();
        verify(userDao, never()).updateEmailVerification(EXISTING_USER_ID, User.EMAIL_VERIFIED);
    }

    @Test
    void resendEmailVerification() {
        var user = givenExistingUser();

        sut.resendEmailVerification(user);

        assertVerificationEmail(mailer);
        verify(userDao).updateEmailVerification(eq(EXISTING_USER_ID), anyString());
    }

    @Test
    void deleteMyProfile() {
        var user = givenExistingUser();

        sut.deleteProfile(user, USER_AGENT);

        verify(userDao).anonymizeUser(eq(EXISTING_USER_ID));
        verify(userDao).addUsernameToBlocklist(USER_NAME);
        verify(authorizationDao).deleteAllByUser(USER_NAME);
    }

    @Test
    void registerUserNameTaken() {
        var user = givenExistingUser();
        var newUser = createNewUser()
                .name(user.getName())
                .build();

        assertThatThrownBy(() -> sut.register(newUser, USER_AGENT))
                .isInstanceOf(ManageProfileUseCase.ProfileConflictException.class);
    }

    @Test
    void registerUserNameBlocked() {
        var newUser = createNewUser().name("Blocked Name").build();
        when(userDao.countBlockedUsername("blockedname")).thenReturn(1);

        assertThatThrownBy(() -> sut.register(newUser, USER_AGENT))
                .isInstanceOf(ManageProfileUseCase.ProfileConflictException.class);
    }

    @Test
    void registerUserEmailTaken() {
        var user = givenExistingUser();
        var newUser = createNewUser()
                .name("othername")
                .email(user.getEmail())
                .build();

        assertThatThrownBy(() -> sut.register(newUser, USER_AGENT))
                .isInstanceOf(ManageProfileUseCase.ProfileConflictException.class);

        assertThat(monitor.getMessages().getFirst()).isEqualTo("Registration for user 'othername' with eMail 'existing@example.com' failed, eMail is already taken\nvia UserAgent");
    }

    @Test
    void registernUserNameTaken() {
        var user = givenExistingUser();
        var newUser = createNewUser()
                .name(user.getName())
                .email("otheremail@example.com")
                .build();

        assertThatThrownBy(() -> sut.register(newUser, USER_AGENT))
                .isInstanceOf(ManageProfileUseCase.ProfileConflictException.class);

        assertThat(monitor.getMessages().getFirst()).isEqualTo("Registration for user '%s' with eMail '%s' failed, name is already taken by different eMail '%s'%nvia %s"
                .formatted(user.getName(), newUser.getEmail(), user.getEmail(), USER_AGENT));
    }

    @Test
    void registerUserWithEmptyName() {
        var newUser = createNewUser()
                .name("")
                .build();

        assertThatThrownBy(() -> sut.register(newUser, USER_AGENT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void assertNewPasswordEmail() {
        verify(mailer, times(1))
                .send(anyString(),
                        anyString(), matches("""
                                Hello,
                                                    
                                your new password is: .*
                                                    
                                Cheers
                                Your Railway-Stations-Team"""));
    }

    private User.UserBuilder createNewUser() {
        var key = "246172676F6E32696424763D3139246D3D36353533362C743D322C703D3124426D4F637165757646794E44754132726B566A6A3177246A7568362F6E6C2F49437A4B475570446E6B674171754A304F7A486A62694F587442542F2B62584D49476300000000000000000000000000000000000000000000000000000000000000";
        return User.builder()
                .name(USER_NAME)
                .email(USER_EMAIL)
                .url("https://link@example.com")
                .license(License.CC0_10)
                .ownPhotos(true)
                .anonymous(false)
                .key(key)
                .admin(false)
                .sendNotifications(true);
    }

    private User givenExistingUser() {
        var user = createNewUser()
                .id(EXISTING_USER_ID)
                .build();
        when(userDao.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userDao.findByNormalizedName(user.getName())).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void resetPasswordUnknownUser() {
        assertThatThrownBy(() -> sut.resetPassword("unknown_user", USER_AGENT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resetPasswordEmptyEmail() {
        when(userDao.findByNormalizedName("nickname")).thenReturn(Optional.of(User.builder().build()));

        assertThatThrownBy(() -> sut.resetPassword("nickname", USER_AGENT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resetPasswordViaUsernameEmailNotVerified() {
        when(userDao.findByNormalizedName("nickname")).thenReturn(Optional.of(User.builder().id(123).name("nickname").email("nickname@example.com").build()));

        sut.resetPassword("nickname", USER_AGENT);

        verify(userDao).updateCredentials(eq(123), anyString());
        assertThat(monitor.getMessages().getFirst()).isEqualTo("Reset Password for 'nickname', email='nickname@example.com'");
        assertNewPasswordEmail();
        verify(userDao).updateEmailVerification(123, User.EMAIL_VERIFIED_AT_NEXT_LOGIN);
        verify(authorizationDao).deleteAllByUser("nickname");
    }

    @Test
    void resetPasswordViaEmailAndEmailVerified() {
        when(userDao.findByEmail("nickname@example.com")).thenReturn(Optional.of(User.builder().id(123).name("nickname").email("nickname@example.com").emailVerification(User.EMAIL_VERIFIED).build()));

        sut.resetPassword("nickname@example.com", USER_AGENT);

        verify(userDao).updateCredentials(eq(123), anyString());
        assertThat(monitor.getMessages().getFirst()).isEqualTo("Reset Password for 'nickname', email='nickname@example.com'");
        assertNewPasswordEmail();
        verify(userDao, never()).updateEmailVerification(123, User.EMAIL_VERIFIED_AT_NEXT_LOGIN);
        verify(authorizationDao).deleteAllByUser("nickname");
    }

}