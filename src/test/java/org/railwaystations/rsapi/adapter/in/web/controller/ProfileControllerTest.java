package org.railwaystations.rsapi.adapter.in.web.controller;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.railwaystations.rsapi.adapter.in.web.ErrorHandlingControllerAdvice;
import org.railwaystations.rsapi.adapter.out.db.UserDao;
import org.railwaystations.rsapi.adapter.out.monitoring.MockMonitor;
import org.railwaystations.rsapi.app.auth.LazySodiumPasswordEncoder;
import org.railwaystations.rsapi.app.auth.RSAuthenticationProvider;
import org.railwaystations.rsapi.app.auth.RSUserDetailsService;
import org.railwaystations.rsapi.app.auth.WebSecurityConfig;
import org.railwaystations.rsapi.core.model.User;
import org.railwaystations.rsapi.core.ports.out.Mailer;
import org.railwaystations.rsapi.core.services.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Optional;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProfileController.class, properties = {"mailVerificationUrl=EMAIL_VERIFICATION_URL"})
@ContextConfiguration(classes={WebMvcTestApplication.class, ErrorHandlingControllerAdvice.class, MockMvcTestConfiguration.class, WebSecurityConfig.class})
@Import({ProfileService.class, RSUserDetailsService.class, RSAuthenticationProvider.class, LazySodiumPasswordEncoder.class})
@ActiveProfiles("mockMvcTest")
public class ProfileControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MockMonitor monitor;

    @MockBean
    private Mailer mailer;

    @MockBean
    private UserDao userDao;

    @BeforeEach
    public void setUp() {
        monitor.getMessages().clear();
    }

    @Test
    public void testRegisterInvalidData() throws Exception {
        final var givenUserProfileWithoutEmail = """
                    { "nickname": "nickname", "link": "https://link@example.com", "license": "CC0", "anonymous": false, "sendNotifications": true, "photoOwner": true }
                """;
        postRegistration(givenUserProfileWithoutEmail)
                .andExpect(status().isBadRequest());
    }

    @NotNull
    private ResultActions postRegistration(final String userProfileJson) throws Exception {
        return mvc.perform(post("/registration")
                        .header("User-Agent", "UserAgent")
                        .contentType("application/json")
                        .content(userProfileJson)
                        .with(csrf()))
                .andExpect(validOpenApi());
    }

    @Test
    public void testRegisterNewUser() throws Exception {
        final var givenUserProfile = """
                    { "nickname": "nickname", "email": "nickname@example.com", "link": "https://link@example.com", "license": "CC0", "anonymous": false, "sendNotifications": true, "photoOwner": true }
                """;
        postRegistration(givenUserProfile)
                .andExpect(status().isAccepted());

        verify(userDao).findByNormalizedName("nickname");
        verify(userDao).findByEmail("nickname@example.com");
        verify(userDao).insert(any(User.class));
        verify(userDao, never()).updateCredentials(anyInt(), anyString());

        assertThat(monitor.getMessages().get(0), equalTo("New registration{nickname='nickname', email='nickname@example.com', license='CC0 1.0 Universell (CC0 1.0)', photoOwner=true, link='https://link@example.com', anonymous=false}\nvia UserAgent"));
        assertNewPasswordEmail();

        verifyNoMoreInteractions(userDao);
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
    public void testRegisterNewUserWithPassword() throws Exception {
        final var givenUserProfileWithPassword = """
                    { "nickname": "nickname", "email": "nickname@example.com", "link": "https://link@example.com", "license": "CC0", "anonymous": false, "sendNotifications": true, "photoOwner": true, "newPassword": "verySecretPassword" }
                """;
        postRegistration(givenUserProfileWithPassword)
                .andExpect(status().isAccepted());

        verify(userDao).findByNormalizedName("nickname");
        verify(userDao).findByEmail("nickname@example.com");
        verify(userDao).insert(any(User.class));
        verify(userDao, never()).updateCredentials(anyInt(), anyString());

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

    @Test
    public void testRegisterNewUserAnonymous() throws Exception {
        final var givenAnonymousUserProfile = """
                    { "nickname": "nickname", "email": "nickname@example.com", "link": "https://link@example.com", "license": "CC0", "anonymous": true, "sendNotifications": true, "photoOwner": true }
                """;
        postRegistration(givenAnonymousUserProfile)
                .andExpect(status().isAccepted());

        assertThat(monitor.getMessages().get(0), equalTo("New registration{nickname='nickname', email='nickname@example.com', license='CC0 1.0 Universell (CC0 1.0)', photoOwner=true, link='https://link@example.com', anonymous=true}\nvia UserAgent"));
    }

    @Test
    public void testRegisterUserNameTaken() throws Exception {
        givenExistingUser();
        final var givenUserProfileWithSameName = """
                    { "nickname": "existing", "email": "other@example.com", "link": "https://link@example.com", "license": "CC0", "anonymous": false, "sendNotifications": true, "photoOwner": true }
                """;
        postRegistration(givenUserProfileWithSameName)
                .andExpect(status().isConflict());
    }

    @Test
    public void testRegisterExistingUserEmailTaken() throws Exception {
        givenExistingUser();
        final var givenUserProfileWithSameEmail = """
                    { "nickname": "othername", "email": "existing@example.com", "link": "https://link@example.com", "license": "CC0", "anonymous": false, "sendNotifications": true, "photoOwner": true }
                """;
        postRegistration(givenUserProfileWithSameEmail)
                .andExpect(status().isConflict());

        assertThat(monitor.getMessages().get(0), equalTo("Registration for user 'othername' with eMail 'existing@example.com' failed, eMail is already taken\nvia UserAgent"));
    }

    @Test
    public void testRegisterExistingUserEmptyName() throws Exception {
        givenExistingUser();
        final var givenUserProfileWithEmptyName = """
                    { "nickname": "", "email": "existing@example.com", "link": "https://link@example.com", "license": "CC0", "anonymous": false, "sendNotifications": true, "photoOwner": true }
                """;
        postRegistration(givenUserProfileWithEmptyName)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetMyProfile() throws Exception {
        givenExistingUser();

        mvc.perform(get("/myProfile")
                        .header("User-Agent", "UserAgent")
                        .contentType("application/json")
                        .secure(true)
                        .with(basicHttpAuthForExistingUser())
                        .with(csrf()))
                .andExpect(validOpenApi())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("existing"));
    }

    private void givenExistingUser() {
        final var key = "246172676F6E32696424763D3139246D3D36353533362C743D322C703D3124426D4F637165757646794E44754132726B566A6A3177246A7568362F6E6C2F49437A4B475570446E6B674171754A304F7A486A62694F587442542F2B62584D49476300000000000000000000000000000000000000000000000000000000000000";
        final var user = new User("existing", null, "CC0", 42, "existing@example.com", true, false, key, false, null, true);
        when(userDao.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userDao.findByNormalizedName(user.getName())).thenReturn(Optional.of(user));
    }

    @NotNull
    private RequestPostProcessor basicHttpAuthForExistingUser() {
        return httpBasic("existing@example.com", "y89zFqkL6hro");
    }

    @Test
    public void testChangePasswordTooShortHeader() throws Exception {
        givenExistingUser();

        postChangePassword("secret", "")
                .andExpect(status().isBadRequest());

        verify(userDao, never()).updateCredentials(anyInt(), anyString());
    }

    @NotNull
    private ResultActions postChangePassword(final String newPasswordHeader, final String newPasswordBody) throws Exception {
        return mvc.perform(post("/changePassword")
                        .header("User-Agent", "UserAgent")
                        .header("New-Password", newPasswordHeader)
                        .contentType("application/json")
                        .content(newPasswordBody)
                        .secure(true)
                        .with(basicHttpAuthForExistingUser())
                        .with(csrf()))
                .andExpect(validOpenApi());
    }

    @Test
    public void testChangePasswordTooShortBody() throws Exception {
        givenExistingUser();

        postChangePassword("", "{\"newPassword\": \"secret\"}")
                .andExpect(status().isBadRequest());

        verify(userDao, never()).updateCredentials(anyInt(), anyString());
        verify(userDao, never()).updateCredentials(anyInt(), anyString());
    }

    @Test
    public void testChangePasswordHeader() throws Exception {
        givenExistingUser();

        postChangePassword("secretlong", "")
                .andExpect(status().isOk());

        final var idCaptor = ArgumentCaptor.forClass(Integer.class);
        final var keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(userDao).updateCredentials(idCaptor.capture(), keyCaptor.capture());

        assertThat(idCaptor.getValue(), equalTo(42));
        assertThat(new LazySodiumPasswordEncoder().matches("secretlong", keyCaptor.getValue()), is(true));
    }

    @Test
    public void testChangePasswordBody() throws Exception {
        givenExistingUser();

        postChangePassword("", "{\"newPassword\": \"secretlong\"}")
                .andExpect(status().isOk());

        final var idCaptor = ArgumentCaptor.forClass(Integer.class);
        final var keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(userDao).updateCredentials(idCaptor.capture(), keyCaptor.capture());

        assertThat(idCaptor.getValue(), equalTo(42));
        assertThat(new LazySodiumPasswordEncoder().matches("secretlong", keyCaptor.getValue()), is(true));
    }

    @Test
    public void testChangePasswordHeaderAndBody() throws Exception {
        givenExistingUser();

        postChangePassword("secretheader", "{\"newPassword\": \"secretbody\"}")
                .andExpect(status().isOk());

        final var idCaptor = ArgumentCaptor.forClass(Integer.class);
        final var keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(userDao).updateCredentials(idCaptor.capture(), keyCaptor.capture());

        assertThat(idCaptor.getValue(), equalTo(42));
        assertThat(new LazySodiumPasswordEncoder().matches("secretbody", keyCaptor.getValue()), is(true));
    }

    @Test
    public void testUpdateMyProfile() throws Exception {
        when(userDao.findByNormalizedName("newname")).thenReturn(Optional.empty());
        givenExistingUser();
        final var newProfileJson = """
                    { "nickname": "new_name", "email": "existing@example.com", "link": "http://twitter.com/", "license": "CC0", "anonymous": true, "sendNotifications": true, "photoOwner": true }
                """;

        postMyProfile(newProfileJson)
                .andExpect(status().isOk());

        verify(userDao).update(new User("new_name", "existing@example.com", "CC0", true, "http://twitter.com/", true, null, true));
    }

    @NotNull
    private ResultActions postMyProfile(final String newProfileJson) throws Exception {
        return mvc.perform(post("/myProfile")
                        .header("User-Agent", "UserAgent")
                        .contentType("application/json")
                        .content(newProfileJson)
                        .secure(true)
                        .with(basicHttpAuthForExistingUser())
                        .with(csrf()))
                .andExpect(validOpenApi());
    }

    @Test
    public void testUpdateMyProfileConflict() throws Exception {
        when(userDao.findByNormalizedName("newname")).thenReturn(Optional.of(new User("@New name", "newname@example.com", null, true, null, false, null, true)));
        givenExistingUser();
        final var newProfileJson = """
                    { "nickname": "new_name", "email": "existing@example.com", "link": "http://twitter.com/", "license": "CC0", "anonymous": true, "sendNotifications": true, "photoOwner": true }
                """;

        postMyProfile(newProfileJson)
                .andExpect(status().isConflict());

        verify(userDao, never()).update(any(User.class));
    }

    @Test
    public void testUpdateMyProfileNewMail() throws Exception {
        when(userDao.findByEmail("newname@example.com")).thenReturn(Optional.empty());
        givenExistingUser();
        final var newProfileJson = """
                    { "nickname": "existing", "email": "newname@example.com", "link": "http://twitter.com/", "license": "CC0", "anonymous": true, "sendNotifications": true, "photoOwner": true }
                """;

        postMyProfile(newProfileJson)
                .andExpect(status().isOk());

        assertVerificationEmail();
        verify(userDao).update(new User("existing", "newname@example.com", "CC0", true, "http://twitter.com/", true, null, true));
    }

    @Test
    public void testNewUploadTokenViaEmail() throws Exception {
        final var user = new User("existing", "existing@example.com", "CC0", true, "https://link@example.com", false, null, true);
        when(userDao.findByNormalizedName(user.getName())).thenReturn(Optional.of(user));
        when(userDao.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        mvc.perform(post("/newUploadToken")
                        .header("User-Agent", "UserAgent")
                        .header("Email", "existing@example.com")
                        .with(csrf()))
                .andExpect(validOpenApi())
                .andExpect(status().isAccepted());

        assertThat(monitor.getMessages().get(0), equalTo("Reset Password for 'existing', email='existing@example.com'"));
    }

    @Test
    public void testResetPasswordViaEmail() throws Exception {
        final var user = new User("existing", "existing@example.com", "CC0", true, "https://link@example.com", false, null, true);
        when(userDao.findByNormalizedName(user.getName())).thenReturn(Optional.of(user));
        when(userDao.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        postResetPassword("existing@example.com")
                .andExpect(status().isAccepted());

        assertThat(monitor.getMessages().get(0), equalTo("Reset Password for 'existing', email='existing@example.com'"));
    }

    @NotNull
    private ResultActions postResetPassword(final String nameOrEmail) throws Exception {
        return mvc.perform(post("/resetPassword")
                        .header("User-Agent", "UserAgent")
                        .header("NameOrEmail", nameOrEmail)
                        .with(csrf()))
                .andExpect(validOpenApi());
    }

    @Test
    public void testResetPasswordViaName() throws Exception {
        final var user = new User("existing", "existing@example.com", "CC0", true, "https://link@example.com", false, null, true);
        when(userDao.findByNormalizedName(user.getName())).thenReturn(Optional.of(user));
        when(userDao.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        postResetPassword("existing")
                .andExpect(status().isAccepted());

        assertThat(monitor.getMessages().get(0), equalTo("Reset Password for 'existing', email='existing@example.com'"));
    }

    @Test
    public void testResetPasswordUserNotFound() throws Exception {
        postResetPassword("doesnt-exist")
                .andExpect(status().isNotFound());
    }

    @Test
    public void testResetPasswordEmailMissing() throws Exception {
        final var user = new User("existing", "", "CC0", true, "https://link@example.com", false, null, true);
        when(userDao.findByNormalizedName(user.getName())).thenReturn(Optional.of(user));
        when(userDao.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        postResetPassword("existing")
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testVerifyEmailSuccess() throws Exception {
        final var token = "verification";
        final var emailVerification = User.EMAIL_VERIFICATION_TOKEN + token;
        final var user = new User("existing","https://link@example.com", "CC0", 42, "existing@example.com", true, false, null, false, emailVerification, true);
        when(userDao.findByEmailVerification(emailVerification)).thenReturn(Optional.of(user));

        getEmailVerification(token)
                .andExpect(status().isOk());

        assertThat(monitor.getMessages().get(0), equalTo("Email verified {nickname='existing', email='existing@example.com'}"));
        verify(userDao).updateEmailVerification(42, User.EMAIL_VERIFIED);
    }

    @NotNull
    private ResultActions getEmailVerification(final String token) throws Exception {
        return mvc.perform(get("/emailVerification/" + token)
                        .header("User-Agent", "UserAgent")
                        .with(csrf()))
                .andExpect(validOpenApi());
    }

    @Test
    public void testVerifyEmailFailed() throws Exception {
        final var token = "verification";
        final var emailVerification = User.EMAIL_VERIFICATION_TOKEN + token;
        final var user = new User("existing","https://link@example.com", "CC0", 42, "existing@example.com", true, false, null, false, emailVerification, true);
        when(userDao.findByEmailVerification(emailVerification)).thenReturn(Optional.of(user));

        getEmailVerification("wrong_token")
                .andExpect(status().isNotFound());

        assertThat(monitor.getMessages().isEmpty(), equalTo(true));
        verify(userDao, never()).updateEmailVerification(42, User.EMAIL_VERIFIED);
    }

    @Test
    public void testResendEmailVerification() throws Exception {
        givenExistingUser();
        mvc.perform(post("/resendEmailVerification")
                        .header("User-Agent", "UserAgent")
                        .secure(true)
                        .with(basicHttpAuthForExistingUser())
                        .with(csrf()))
                .andExpect(validOpenApi())
                .andExpect(status().isOk());

        assertVerificationEmail();
        verify(userDao).updateEmailVerification(eq(42), startsWith(User.EMAIL_VERIFICATION_TOKEN));
    }

    private ResultMatcher validOpenApi() {
        return openApi().isValid("static/openapi.yaml");
    }

}
