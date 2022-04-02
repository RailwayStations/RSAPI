package org.railwaystations.rsapi.adapter.in.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.railwaystations.rsapi.adapter.out.db.UserDao;
import org.railwaystations.rsapi.adapter.out.monitoring.MockMonitor;
import org.railwaystations.rsapi.app.auth.AuthUser;
import org.railwaystations.rsapi.app.auth.LazySodiumPasswordEncoder;
import org.railwaystations.rsapi.core.model.ChangePassword;
import org.railwaystations.rsapi.core.model.User;
import org.railwaystations.rsapi.core.ports.out.Mailer;
import org.railwaystations.rsapi.core.services.ProfileService;

import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ProfileControllerTest {

    private static final String EMAIL_VERIFICATION_URL = "EMAIL_VERIFICATION_URL";

    private MockMonitor monitor;
    private Mailer mailer;
    private ProfileController resource;
    private UserDao userDao;

    @BeforeEach
    public void setUp() {
        monitor = new MockMonitor();
        mailer = mock(Mailer.class);
        userDao = mock(UserDao.class);

        resource = new ProfileController(new ProfileService(monitor, mailer, userDao, EMAIL_VERIFICATION_URL, new LazySodiumPasswordEncoder()));
    }

    @Test
    public void testRegisterInvalidData() {
        final var registration = new User("nickname", null, null, true, "https://link@example.com", false, null, true);
        final var response = resource.register("UserAgent", registration);

        assertThat(response.getStatusCodeValue(), equalTo(400));
    }

    @Test
    public void testRegisterNewUser() {
        final var registration = new User("nickname", "nickname@example.com", "CC0", true, "https://link@example.com", false, null, true);
        final var response = resource.register("UserAgent", registration);
        verify(userDao).findByNormalizedName("nickname");
        verify(userDao).findByEmail("nickname@example.com");
        verify(userDao).insert(any(User.class));
        verify(userDao, never()).updateCredentials(anyInt(), anyString());

        assertThat(response.getStatusCodeValue(), equalTo(202));
        assertThat(monitor.getMessages().get(0), equalTo("New registration{nickname='nickname', email='nickname@example.com', license='CC0 1.0 Universell (CC0 1.0)', photoOwner=true, link='https://link@example.com', anonymous=false}\nvia UserAgent"));
        assertNewPasswordEmail();

        verifyNoMoreInteractions(userDao);
    }

    @Test
    public void testRegisterNewUserWithPassword() {
        final var registration = new User("nickname", "nickname@example.com", "CC0", true, "https://link@example.com", false, "verySecretPassword", true);
        final var response = resource.register("UserAgent", registration);
        verify(userDao).findByNormalizedName("nickname");
        verify(userDao).findByEmail("nickname@example.com");
        verify(userDao).insert(any(User.class));
        verify(userDao, never()).updateCredentials(anyInt(), anyString());

        assertThat(response.getStatusCodeValue(), equalTo(202));
        assertThat(monitor.getMessages().get(0), equalTo("New registration{nickname='nickname', email='nickname@example.com', license='CC0 1.0 Universell (CC0 1.0)', photoOwner=true, link='https://link@example.com', anonymous=false}\nvia UserAgent"));
        assertVerificationEmail();

        verifyNoMoreInteractions(userDao);
    }

    private void assertVerificationEmail() {
        verify(mailer, times(1))
            .send(anyString(),
                anyString(),matches("""
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

    private void assertNewPasswordEmail() {
        verify(mailer, times(1))
            .send(anyString(),
                anyString(),matches("""
                    Hello,
                    
                    your new password is: .*
                    
                    Cheers
                    Your Railway-Stations-Team
                    
                    ---
                    Hallo,
                    
                    Dein neues Passwort lautet: .*
                    
                    Viele Grüße
                    Dein Bahnhofsfoto-Team"""));
    }

    @Test
    public void testRegisterNewUserAnonymous() {
        final var registration = new User("nickname", "nickname@example.com", "CC0", true, "https://link@example.com", true, null, true);
        final var response = resource.register("UserAgent", registration);

        assertThat(response.getStatusCodeValue(), equalTo(202));
        assertThat(monitor.getMessages().get(0), equalTo("New registration{nickname='nickname', email='nickname@example.com', license='CC0 1.0 Universell (CC0 1.0)', photoOwner=true, link='https://link@example.com', anonymous=true}\nvia UserAgent"));
    }

    @Test
    public void testRegisterNewUserNameTaken() {
        when(userDao.findByNormalizedName("existing")).thenReturn(Optional.of(new User("existing", "existing@example.com", "CC0", true, "https://link@example.com", false, null, true)));
        final var registration = new User("existing", "other@example.com", "CC0", true, "https://link@example.com", false, null, true);
        final var response = resource.register("UserAgent", registration);

        assertThat(response.getStatusCodeValue(), equalTo(409));
    }

    @Test
    public void testRegisterExistingUserEmailTaken() {
        final var user = new User("existing", "existing@example.com", "CC0", true, "https://link@example.com", false, null, true);
        when(userDao.findByNormalizedName(user.getName())).thenReturn(Optional.of(user));
        when(userDao.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        final var response = resource.register("UserAgent", user);

        assertThat(response.getStatusCodeValue(), equalTo(409));
        assertThat(monitor.getMessages().get(0), equalTo("Registration for user 'existing' with eMail 'existing@example.com' failed, eMail is already taken\nvia UserAgent"));
    }

    @Test
    public void testRegisterExistingUserNameTaken() {
        when(userDao.findByNormalizedName("existing")).thenReturn(Optional.of(new User("existing", "other@example.com", "CC0", true, "https://link@example.com", false, null, true)));
        final var registration = new User("existing", "existing@example.com", "CC0", true, "https://link@example.com", false, null, true);
        final var response = resource.register("UserAgent", registration);

        assertThat(response.getStatusCodeValue(), equalTo(409));
        assertThat(monitor.getMessages().get(0), equalTo("Registration for user 'existing' with eMail 'existing@example.com' failed, name is already taken by different eMail 'other@example.com'\nvia UserAgent"));
    }

    @Test
    public void testRegisterExistingUserEmptyName() {
        when(userDao.findByNormalizedName("existing")).thenReturn(Optional.of(new User("existing", "other@example.com", "CC0", true, "https://link@example.com", false, null, true)));
        final var registration = new User("", "existing@example.com", "CC0", true, "https://link@example.com", false, null, true);
        final var response = resource.register("UserAgent", registration);

        assertThat(response.getStatusCodeValue(), equalTo(400));
    }

    @Test
    public void testGetMyProfile() {
        final var user = new User("existing", "existing@example.com", null, true, null, false, null, true);
        final var response = resource.getMyProfile(new AuthUser(user, Collections.emptyList()));

        assertThat(response.getStatusCodeValue(), equalTo(200));
        assertThat(response.getBody(), sameInstance(user));
    }

    @Test
    public void testChangePasswordTooShortHeader() {
        final var user = new User("existing", "existing@example.com", null, true, null, false, null, true);
        final var response = resource.changePassword(new AuthUser(user, Collections.emptyList()), "secret", null);
        verify(userDao, never()).updateCredentials(anyInt(), anyString());

        assertThat(response.getStatusCodeValue(), equalTo(400));
    }

    @Test
    public void testChangePasswordTooShortBody() {
        final var user = new User("existing", "existing@example.com", null, true, null, false, null, true);
        final var response = resource.changePassword(new AuthUser(user, Collections.emptyList()), null, new ChangePassword("secret"));
        verify(userDao, never()).updateCredentials(anyInt(), anyString());

        assertThat(response.getStatusCodeValue(), equalTo(400));
    }

    @Test
    public void testChangePasswordHeader() {
        final var user = new User("existing", "existing@example.com", null, true, null, false, null, true);
        user.setId(4711);
        final var idCaptor = ArgumentCaptor.forClass(Integer.class);
        final var keyCaptor = ArgumentCaptor.forClass(String.class);
        final var response = resource.changePassword(new AuthUser(user, Collections.emptyList()), "secretlong", null);
        verify(userDao).updateCredentials(idCaptor.capture(), keyCaptor.capture());

        assertThat(response.getStatusCodeValue(), equalTo(200));
        assertThat(idCaptor.getValue(), equalTo(4711));
        assertThat(new LazySodiumPasswordEncoder().matches("secretlong", keyCaptor.getValue()), is(true));
    }

    @Test
    public void testChangePasswordBody() {
        final var user = new User("existing", "existing@example.com", null, true, null, false, null, true);
        user.setId(4711);
        final var idCaptor = ArgumentCaptor.forClass(Integer.class);
        final var keyCaptor = ArgumentCaptor.forClass(String.class);
        final var response = resource.changePassword(new AuthUser(user, Collections.emptyList()), null, new ChangePassword("secretlong"));
        verify(userDao).updateCredentials(idCaptor.capture(), keyCaptor.capture());

        assertThat(response.getStatusCodeValue(), equalTo(200));
        assertThat(idCaptor.getValue(), equalTo(4711));
        assertThat(new LazySodiumPasswordEncoder().matches("secretlong", keyCaptor.getValue()), is(true));
    }

    @Test
    public void testChangePasswordHeaderAndBody() {
        final var user = new User("existing", "existing@example.com", null, true, null, false, null, true);
        user.setId(4711);
        final var idCaptor = ArgumentCaptor.forClass(Integer.class);
        final var keyCaptor = ArgumentCaptor.forClass(String.class);
        final var response = resource.changePassword(new AuthUser(user, Collections.emptyList()), "secretheader", new ChangePassword("secretbody"));
        verify(userDao).updateCredentials(idCaptor.capture(), keyCaptor.capture());

        assertThat(response.getStatusCodeValue(), equalTo(200));
        assertThat(idCaptor.getValue(), equalTo(4711));
        assertThat(new LazySodiumPasswordEncoder().matches("secretbody", keyCaptor.getValue()), is(true));
    }

    @Test
    public void testUpdateMyProfile() {
        when(userDao.findByNormalizedName("newname")).thenReturn(Optional.empty());
        final var user = new User("existing", "existing@example.com", null, true, null, false, null, true);
        final var newProfile = new User("new_name", "existing@example.com", "CC0", true, "http://twitter.com/", true, null, true);
        final var response = resource.updateMyProfile("UserAgent", newProfile, new AuthUser(user, Collections.emptyList()));

        assertThat(response.getStatusCodeValue(), equalTo(200));
        verify(userDao).update(newProfile);
    }

    @Test
    public void testUpdateMyProfileConflict() {
        when(userDao.findByNormalizedName("newname")).thenReturn(Optional.of(new User("@New name", "newname@example.com", null, true, null, false, null, true)));
        final var user = new User("existing", "existing@example.com", null, true, null, false, null, true);
        final var newProfile = new User("new_name", "existing@example.com", "CC0", true, "http://twitter.com/", true, null, true);
        final var response = resource.updateMyProfile("UserAgent", newProfile, new AuthUser(user, Collections.emptyList()));

        assertThat(response.getStatusCodeValue(), equalTo(409));
        verify(userDao, never()).update(newProfile);
    }

    @Test
    public void testUpdateMyProfileNewMail() {
        when(userDao.findByEmail("newname@example.com")).thenReturn(Optional.empty());
        final var user = new User("existing", "existing@example.com", null, true, null, false, null, true);
        final var newProfile = new User("existing", "newname@example.com", "CC0", true, "http://twitter.com/", true, null, true);
        final var response = resource.updateMyProfile("UserAgent", newProfile, new AuthUser(user, Collections.emptyList()));

        assertThat(response.getStatusCodeValue(), equalTo(200));
        assertVerificationEmail();
        verify(userDao).update(newProfile);
    }

    @Test
    public void testNewUploadTokenViaEmail() {
        final var user = new User("existing", "existing@example.com", "CC0", true, "https://link@example.com", false, null, true);
        when(userDao.findByNormalizedName(user.getName())).thenReturn(Optional.of(user));
        when(userDao.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        final var response = resource.newUploadToken("UserAgent", "existing@example.com");

        assertThat(response.getStatusCodeValue(), equalTo(202));
        assertThat(monitor.getMessages().get(0), equalTo("Reset Password for 'existing', email='existing@example.com'"));
    }

    @Test
    public void testNewUploadTokenViaName() {
        final var user = new User("existing", "existing@example.com", "CC0", true, "https://link@example.com", false, null, true);
        when(userDao.findByNormalizedName(user.getName())).thenReturn(Optional.of(user));
        when(userDao.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        final var response = resource.newUploadToken("UserAgent", "existing");

        assertThat(response.getStatusCodeValue(), equalTo(202));
        assertThat(monitor.getMessages().get(0), equalTo("Reset Password for 'existing', email='existing@example.com'"));
    }

    @Test
    public void testNewUploadTokenNotFound() {
        final var response = resource.newUploadToken("UserAgent", "doesnt-exist");

        assertThat(response.getStatusCodeValue(), equalTo(404));
    }

    @Test
    public void testNewUploadTokenEmailMissing() {
        final var user = new User("existing", "", "CC0", true, "https://link@example.com", false, null, true);
        when(userDao.findByNormalizedName(user.getName())).thenReturn(Optional.of(user));
        when(userDao.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        final var response = resource.newUploadToken("UserAgent", "existing");

        assertThat(response.getStatusCodeValue(), equalTo(400));
    }

    @Test
    public void testVerifyEmailSuccess() {
        final var token = "verification";
        final var emailVerification = User.EMAIL_VERIFICATION_TOKEN + token;
        final var user = new User("existing","https://link@example.com", "CC0", 42, "existing@example.com", true, false, null, false, emailVerification, true);
        when(userDao.findByEmailVerification(emailVerification)).thenReturn(Optional.of(user));
        final var response = resource.emailVerification(token);

        assertThat(response.getStatusCodeValue(), equalTo(200));
        assertThat(monitor.getMessages().get(0), equalTo("Email verified {nickname='existing', email='existing@example.com'}"));
        verify(userDao).updateEmailVerification(42, User.EMAIL_VERIFIED);
    }

    @Test
    public void testVerifyEmailFailed() {
        final var token = "verification";
        final var emailVerification = User.EMAIL_VERIFICATION_TOKEN + token;
        final var user = new User("existing","https://link@example.com", "CC0", 42, "existing@example.com", true, false, null, false, emailVerification, true);
        when(userDao.findByEmailVerification(emailVerification)).thenReturn(Optional.of(user));
        final var response = resource.emailVerification("wrong_token");

        assertThat(response.getStatusCodeValue(), equalTo(404));
        assertThat(monitor.getMessages().isEmpty(), equalTo(true));
        verify(userDao, never()).updateEmailVerification(42, User.EMAIL_VERIFIED);
    }

    @Test
    public void testResendEmailVerification() {
        when(userDao.findByEmail("newname@example.com")).thenReturn(Optional.empty());
        final var user = new User("existing","https://link@example.com", "CC0", 42, "existing@example.com", true, false, null, false, User.EMAIL_VERIFIED_AT_NEXT_LOGIN, true);
        final var response = resource.resendEmailVerification(new AuthUser(user, Collections.emptyList()));

        assertThat(response.getStatusCodeValue(), equalTo(200));
        assertThat(user.getEmailVerification().startsWith(User.EMAIL_VERIFICATION_TOKEN), is(true));
        assertVerificationEmail();
        verify(userDao).updateEmailVerification(user.getId(), user.getEmailVerification());
    }

}
