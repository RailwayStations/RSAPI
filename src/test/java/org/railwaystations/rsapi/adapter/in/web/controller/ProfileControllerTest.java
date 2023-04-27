package org.railwaystations.rsapi.adapter.in.web.controller;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.railwaystations.rsapi.adapter.in.web.ErrorHandlingControllerAdvice;
import org.railwaystations.rsapi.adapter.out.db.OAuth2AuthorizationDao;
import org.railwaystations.rsapi.adapter.out.db.UserDao;
import org.railwaystations.rsapi.adapter.out.monitoring.MockMonitor;
import org.railwaystations.rsapi.app.auth.LazySodiumPasswordEncoder;
import org.railwaystations.rsapi.app.auth.RSAuthenticationProvider;
import org.railwaystations.rsapi.app.auth.RSUserDetailsService;
import org.railwaystations.rsapi.app.auth.WebSecurityConfig;
import org.railwaystations.rsapi.core.model.License;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.railwaystations.rsapi.utils.OpenApiValidatorUtil.validOpenApiResponse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProfileController.class, properties = {"mailVerificationUrl=EMAIL_VERIFICATION_URL"})
@ContextConfiguration(classes = {WebMvcTestApplication.class, ErrorHandlingControllerAdvice.class, MockMvcTestConfiguration.class, WebSecurityConfig.class})
@Import({ProfileService.class, RSUserDetailsService.class, RSAuthenticationProvider.class, LazySodiumPasswordEncoder.class})
@ActiveProfiles("mockMvcTest")
class ProfileControllerTest {

    public static final String EXISTING_USER_NAME = "existing";
    public static final String EXISTING_EMAIL = "existing@example.com";
    public static final int EXISTING_USER_ID = 42;
    @Autowired
    private MockMvc mvc;

    @Autowired
    private MockMonitor monitor;

    @MockBean
    private Mailer mailer;

    @MockBean
    private UserDao userDao;

    @MockBean
    private OAuth2AuthorizationDao authorizationDao;

    @BeforeEach
    public void setUp() {
        monitor.getMessages().clear();
    }

    @Test
    void testRegisterInvalidData() throws Exception {
        var givenUserProfileWithoutEmail = """
                    { "nickname": "nickname", "link": "https://link@example.com", "license": "CC0", "anonymous": false, "sendNotifications": true, "photoOwner": true }
                """;
        postRegistration(givenUserProfileWithoutEmail).andExpect(status().isBadRequest());
    }

    @NotNull
    private ResultActions postRegistration(String userProfileJson) throws Exception {
        return mvc.perform(post("/registration")
                        .header("User-Agent", "UserAgent")
                        .contentType("application/json")
                        .content(userProfileJson)
                        .with(csrf()))
                .andExpect(validOpenApiResponse());
    }

    @Test
    void testRegisterNewUser() throws Exception {
        var givenUserProfile = """
                    { "nickname": "nickname", "email": "nickname@example.com", "link": "https://link@example.com", "license": "CC0", "anonymous": false, "sendNotifications": true, "photoOwner": true }
                """;
        postRegistration(givenUserProfile).andExpect(status().isAccepted());

        verify(userDao).findByNormalizedName("nickname");
        verify(userDao).countBlockedUsername("nickname");
        verify(userDao).findByEmail("nickname@example.com");
        verify(userDao).insert(any(User.class), anyString(), anyString());
        verify(userDao, never()).updateCredentials(anyInt(), anyString());

        assertThat(monitor.getMessages().get(0)).isEqualTo("New registration{nickname='nickname', email='nickname@example.com', license='CC0_10', photoOwner=true, link='https://link@example.com', anonymous=false}\nvia UserAgent");
        assertNewPasswordEmail();

        verifyNoMoreInteractions(userDao);
    }

    private void assertNewPasswordEmail() {
        verify(mailer, times(1))
                .send(anyString(),
                        anyString(), matches("""
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
    void testRegisterNewUserWithPassword() throws Exception {
        var givenUserProfileWithPassword = """
                    { "nickname": "nickname", "email": "nickname@example.com", "link": "https://link@example.com", "license": "CC0", "anonymous": false, "sendNotifications": true, "photoOwner": true, "newPassword": "verySecretPassword" }
                """;
        postRegistration(givenUserProfileWithPassword).andExpect(status().isAccepted());

        verify(userDao).findByNormalizedName("nickname");
        verify(userDao).countBlockedUsername("nickname");
        verify(userDao).findByEmail("nickname@example.com");
        verify(userDao).insert(any(User.class), anyString(), anyString());
        verify(userDao, never()).updateCredentials(anyInt(), anyString());

        assertThat(monitor.getMessages().get(0)).isEqualTo("New registration{nickname='nickname', email='nickname@example.com', license='CC0_10', photoOwner=true, link='https://link@example.com', anonymous=false}\nvia UserAgent");
        assertVerificationEmail();

        verifyNoMoreInteractions(userDao);
    }

    private void assertVerificationEmail() {
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
    void testRegisterNewUserAnonymous() throws Exception {
        var givenAnonymousUserProfile = """
                    { "nickname": "nickname", "email": "nickname@example.com", "link": "https://link@example.com", "license": "CC0", "anonymous": true, "sendNotifications": true, "photoOwner": true }
                """;
        postRegistration(givenAnonymousUserProfile).andExpect(status().isAccepted());

        assertThat(monitor.getMessages().get(0)).isEqualTo("New registration{nickname='nickname', email='nickname@example.com', license='CC0_10', photoOwner=true, link='https://link@example.com', anonymous=true}\nvia UserAgent");
    }

    @Test
    void testRegisterUserNameTaken() throws Exception {
        givenExistingUser();
        var givenUserProfileWithSameName = """
                    { "nickname": "%s", "email": "other@example.com", "link": "https://link@example.com", "license": "CC0", "anonymous": false, "sendNotifications": true, "photoOwner": true }
                """.formatted(EXISTING_USER_NAME);
        postRegistration(givenUserProfileWithSameName).andExpect(status().isConflict());
    }

    @Test
    void testRegisterUserNameBlocked() throws Exception {
        when(userDao.countBlockedUsername("blockedname")).thenReturn(1);
        givenExistingUser();
        var givenUserProfileWithBlockedName = """
                    { "nickname": "%s", "email": "other@example.com", "link": "https://link@example.com", "license": "CC0", "anonymous": false, "sendNotifications": true, "photoOwner": true }
                """.formatted("Blocked Name");
        postRegistration(givenUserProfileWithBlockedName).andExpect(status().isConflict());
    }

    @Test
    void testRegisterExistingUserEmailTaken() throws Exception {
        givenExistingUser();
        var givenUserProfileWithSameEmail = """
                    { "nickname": "othername", "email": "%s", "link": "https://link@example.com", "license": "CC0", "anonymous": false, "sendNotifications": true, "photoOwner": true }
                """.formatted(EXISTING_EMAIL);
        postRegistration(givenUserProfileWithSameEmail).andExpect(status().isConflict());

        assertThat(monitor.getMessages().get(0)).isEqualTo("Registration for user 'othername' with eMail 'existing@example.com' failed, eMail is already taken\nvia UserAgent");
    }

    @Test
    void testRegisterExistingUserEmptyName() throws Exception {
        givenExistingUser();
        var givenUserProfileWithEmptyName = """
                    { "nickname": "", "email": "%s", "link": "https://link@example.com", "license": "CC0", "anonymous": false, "sendNotifications": true, "photoOwner": true }
                """.formatted(EXISTING_EMAIL);
        postRegistration(givenUserProfileWithEmptyName).andExpect(status().isBadRequest());
    }

    @Test
    void testGetMyProfile() throws Exception {
        givenExistingUser();

        mvc.perform(get("/myProfile")
                        .header("User-Agent", "UserAgent")
                        .contentType("application/json")
                        .secure(true)
                        .with(basicHttpAuthForExistingUser())
                        .with(csrf()))
                .andExpect(validOpenApiResponse())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value(EXISTING_USER_NAME));
    }

    private void givenExistingUser() {
        var key = "246172676F6E32696424763D3139246D3D36353533362C743D322C703D3124426D4F637165757646794E44754132726B566A6A3177246A7568362F6E6C2F49437A4B475570446E6B674171754A304F7A486A62694F587442542F2B62584D49476300000000000000000000000000000000000000000000000000000000000000";
        var user = User.builder()
                .name(EXISTING_USER_NAME)
                .license(License.CC0_10)
                .id(EXISTING_USER_ID)
                .email(EXISTING_EMAIL)
                .ownPhotos(true)
                .anonymous(false)
                .key(key)
                .admin(false)
                .sendNotifications(true).build();
        when(userDao.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userDao.findByNormalizedName(user.getName())).thenReturn(Optional.of(user));
    }

    @NotNull
    private RequestPostProcessor basicHttpAuthForExistingUser() {
        return httpBasic(EXISTING_EMAIL, "y89zFqkL6hro");
    }

    @Test
    void testChangePasswordTooShortHeader() throws Exception {
        givenExistingUser();

        postChangePassword("secret", null)
                .andExpect(status().isBadRequest());

        verify(userDao, never()).updateCredentials(anyInt(), anyString());
        verify(authorizationDao, never()).deleteAllByUser(EXISTING_USER_NAME);
    }

    @NotNull
    private ResultActions postChangePassword(String newPasswordHeader, String newPasswordBody) throws Exception {
        MockHttpServletRequestBuilder action = post("/changePassword")
                .header("User-Agent", "UserAgent")
                .secure(true)
                .with(basicHttpAuthForExistingUser())
                .with(csrf());

        if (newPasswordHeader != null) {
            action = action.header("New-Password", newPasswordHeader);
        }

        if (newPasswordBody != null) {
            action = action.contentType("application/json")
                    .content(newPasswordBody);

        }

        return mvc.perform(action).andExpect(validOpenApiResponse());
    }

    @Test
    void testChangePasswordTooShortBody() throws Exception {
        givenExistingUser();

        postChangePassword(null, "{\"newPassword\": \"secret\"}")
                .andExpect(status().isBadRequest());

        verify(userDao, never()).updateCredentials(anyInt(), anyString());
        verify(userDao, never()).updateCredentials(anyInt(), anyString());
        verify(authorizationDao, never()).deleteAllByUser(EXISTING_USER_NAME);
    }

    @Test
    void testChangePasswordHeader() throws Exception {
        givenExistingUser();

        postChangePassword("secretlong", null)
                .andExpect(status().isOk());

        var idCaptor = ArgumentCaptor.forClass(Integer.class);
        var keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(userDao).updateCredentials(idCaptor.capture(), keyCaptor.capture());
        verify(authorizationDao).deleteAllByUser(EXISTING_USER_NAME);

        assertThat(idCaptor.getValue()).isEqualTo(EXISTING_USER_ID);
        assertThat(new LazySodiumPasswordEncoder().matches("secretlong", keyCaptor.getValue())).isTrue();
    }

    @Test
    void testChangePasswordBody() throws Exception {
        givenExistingUser();

        postChangePassword(null, "{\"newPassword\": \"secretlong\"}")
                .andExpect(status().isOk());

        var idCaptor = ArgumentCaptor.forClass(Integer.class);
        var keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(userDao).updateCredentials(idCaptor.capture(), keyCaptor.capture());
        verify(authorizationDao).deleteAllByUser(EXISTING_USER_NAME);

        assertThat(idCaptor.getValue()).isEqualTo(EXISTING_USER_ID);
        assertThat(new LazySodiumPasswordEncoder().matches("secretlong", keyCaptor.getValue())).isTrue();
    }

    @Test
    void testChangePasswordHeaderAndBody() throws Exception {
        givenExistingUser();

        postChangePassword("secretheader", "{\"newPassword\": \"secretbody\"}")
                .andExpect(status().isOk());

        var idCaptor = ArgumentCaptor.forClass(Integer.class);
        var keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(userDao).updateCredentials(idCaptor.capture(), keyCaptor.capture());
        verify(authorizationDao).deleteAllByUser(EXISTING_USER_NAME);

        assertThat(idCaptor.getValue()).isEqualTo(EXISTING_USER_ID);
        assertThat(new LazySodiumPasswordEncoder().matches("secretbody", keyCaptor.getValue())).isTrue();
    }

    @Test
    void testUpdateMyProfile() throws Exception {
        when(userDao.findByNormalizedName("newname")).thenReturn(Optional.empty());
        givenExistingUser();
        var newProfileJson = """
                    { "nickname": "new_name", "email": "%s", "link": "http://twitter.com/", "license": "CC0", "anonymous": true, "sendNotifications": true, "photoOwner": true }
                """.formatted(EXISTING_EMAIL);

        postMyProfileWithOpenApiValidation(newProfileJson).andExpect(status().isOk());

        var user = User.builder()
                .id(EXISTING_USER_ID)
                .name("new_name")
                .license(License.CC0_10)
                .email(EXISTING_EMAIL)
                .ownPhotos(true)
                .url("http://twitter.com/")
                .anonymous(true)
                .sendNotifications(true).build();
        verify(userDao).update(user.getId(), user);
    }

    @NotNull
    private ResultActions postMyProfileWithOpenApiValidation(String newProfileJson) throws Exception {
        return postMyProfile(newProfileJson)
                .andExpect(validOpenApiResponse());
    }

    @NotNull
    private ResultActions postMyProfile(String newProfileJson) throws Exception {
        return mvc.perform(post("/myProfile")
                .header("User-Agent", "UserAgent")
                .contentType("application/json")
                .content(newProfileJson)
                .secure(true)
                .with(basicHttpAuthForExistingUser())
                .with(csrf()));
    }

    @Test
    void testUpdateMyProfileConflict() throws Exception {
        var user = User.builder()
                .name("@New name")
                .email("newname@example.com")
                .sendNotifications(true).build();
        when(userDao.findByNormalizedName("newname")).thenReturn(Optional.of(user));
        givenExistingUser();
        var newProfileJson = """
                    { "nickname": "new_name", "email": "%s", "link": "http://twitter.com/", "license": "CC0", "anonymous": true, "sendNotifications": true, "photoOwner": true }
                """.formatted(EXISTING_EMAIL);

        postMyProfileWithOpenApiValidation(newProfileJson).andExpect(status().isConflict());

        verify(userDao, never()).update(eq(user.getId()), any(User.class));
    }

    @Test
    void testUpdateMyProfileNameTooLong() throws Exception {
        var user = User.builder()
                .name("@New name")
                .email("newname@example.com")
                .sendNotifications(true).build();
        givenExistingUser();
        var newProfileJson = """
                    { "nickname": "A very long name with a lot of extra words to overfill the database column", "email": "%s", "link": "http://twitter.com/", "license": "CC0", "anonymous": true, "sendNotifications": true, "photoOwner": true }
                """.formatted(EXISTING_EMAIL);

        postMyProfile(newProfileJson).andExpect(status().isBadRequest());

        verify(userDao, never()).update(eq(user.getId()), any(User.class));
    }

    @Test
    void testUpdateMyProfileNewMail() throws Exception {
        when(userDao.findByEmail("newname@example.com")).thenReturn(Optional.empty());
        givenExistingUser();
        var newProfileJson = """
                    { "nickname": "%s", "email": "newname@example.com", "link": "http://twitter.com/", "license": "CC0", "anonymous": true, "sendNotifications": true, "photoOwner": true }
                """.formatted(EXISTING_USER_NAME);

        postMyProfileWithOpenApiValidation(newProfileJson).andExpect(status().isOk());

        assertVerificationEmail();
        var user = User.builder()
                .id(EXISTING_USER_ID)
                .name(EXISTING_USER_NAME)
                .license(License.CC0_10)
                .email("newname@example.com")
                .ownPhotos(true)
                .anonymous(false)
                .url("http://twitter.com/")
                .sendNotifications(true).build();
        verify(userDao).update(user.getId(), user);
    }

    @Test
    void testResetPasswordViaEmail() throws Exception {
        var user = User.builder()
                .name(EXISTING_USER_NAME)
                .license(License.CC0_10)
                .email(EXISTING_EMAIL)
                .ownPhotos(true)
                .anonymous(false)
                .url("https://link@example.com")
                .sendNotifications(true).build();
        when(userDao.findByNormalizedName(user.getName())).thenReturn(Optional.of(user));
        when(userDao.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        postResetPassword(EXISTING_EMAIL).andExpect(status().isAccepted());

        verify(authorizationDao).deleteAllByUser(EXISTING_USER_NAME);
        assertThat(monitor.getMessages().get(0)).isEqualTo(String.format("Reset Password for '%s', email='%s'", EXISTING_USER_NAME, EXISTING_EMAIL));
    }

    @NotNull
    private ResultActions postResetPassword(String nameOrEmail) throws Exception {
        return mvc.perform(post("/resetPassword")
                        .header("User-Agent", "UserAgent")
                        .header("NameOrEmail", nameOrEmail)
                        .with(csrf()))
                .andExpect(validOpenApiResponse());
    }

    @Test
    void testResetPasswordViaName() throws Exception {
        var user = User.builder()
                .name(EXISTING_USER_NAME)
                .license(License.CC0_10)
                .email(EXISTING_EMAIL)
                .ownPhotos(true)
                .anonymous(false)
                .url("https://link@example.com")
                .sendNotifications(true).build();
        when(userDao.findByNormalizedName(user.getName())).thenReturn(Optional.of(user));
        when(userDao.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        postResetPassword(EXISTING_USER_NAME).andExpect(status().isAccepted());

        verify(authorizationDao).deleteAllByUser(EXISTING_USER_NAME);
        assertThat(monitor.getMessages().get(0)).isEqualTo(String.format("Reset Password for '%s', email='%s'", EXISTING_USER_NAME, EXISTING_EMAIL));
    }

    @Test
    void testResetPasswordUserNotFound() throws Exception {
        postResetPassword("doesnt-exist").andExpect(status().isBadRequest());
        verify(authorizationDao, never()).deleteAllByUser("doesnt-exist");
    }

    @Test
    void testResetPasswordEmailMissing() throws Exception {
        var user = User.builder()
                .name(EXISTING_USER_NAME)
                .build();
        when(userDao.findByNormalizedName(user.getName())).thenReturn(Optional.of(user));
        when(userDao.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        postResetPassword(EXISTING_USER_NAME)
                .andExpect(status().isBadRequest());
        verify(authorizationDao, never()).deleteAllByUser(EXISTING_USER_NAME);
    }

    @Test
    void testVerifyEmailSuccess() throws Exception {
        var token = "verification";
        var user = User.builder()
                .id(EXISTING_USER_ID)
                .name(EXISTING_USER_NAME)
                .license(License.CC0_10)
                .email(EXISTING_EMAIL)
                .ownPhotos(true)
                .anonymous(false)
                .url("https://link@example.com")
                .emailVerification(token)
                .sendNotifications(true).build();
        when(userDao.findByEmailVerification(token)).thenReturn(Optional.of(user));

        getEmailVerification(token)
                .andExpect(status().isOk());

        assertThat(monitor.getMessages().get(0)).isEqualTo(String.format("Email verified {nickname='%s', email='%s'}", EXISTING_USER_NAME, EXISTING_EMAIL));
        verify(userDao).updateEmailVerification(EXISTING_USER_ID, User.EMAIL_VERIFIED);
    }

    @NotNull
    private ResultActions getEmailVerification(String token) throws Exception {
        return mvc.perform(get("/emailVerification/" + token)
                        .header("User-Agent", "UserAgent")
                        .with(csrf()))
                .andExpect(validOpenApiResponse());
    }

    @Test
    void testVerifyEmailFailed() throws Exception {
        var token = "verification";
        var user = User.builder()
                .id(EXISTING_USER_ID)
                .name(EXISTING_USER_NAME)
                .license(License.CC0_10)
                .email(EXISTING_EMAIL)
                .ownPhotos(true)
                .anonymous(false)
                .url("https://link@example.com")
                .emailVerification(token)
                .sendNotifications(true).build();
        when(userDao.findByEmailVerification(token)).thenReturn(Optional.of(user));

        getEmailVerification("wrong_token").andExpect(status().isNotFound());

        assertThat(monitor.getMessages().isEmpty()).isTrue();
        verify(userDao, never()).updateEmailVerification(EXISTING_USER_ID, User.EMAIL_VERIFIED);
    }

    @Test
    void testResendEmailVerification() throws Exception {
        givenExistingUser();
        mvc.perform(post("/resendEmailVerification")
                        .header("User-Agent", "UserAgent")
                        .secure(true)
                        .with(basicHttpAuthForExistingUser())
                        .with(csrf()))
                .andExpect(validOpenApiResponse())
                .andExpect(status().isOk());

        assertVerificationEmail();
        verify(userDao).updateEmailVerification(eq(EXISTING_USER_ID), anyString());
    }

    @Test
    void testDeleteMyProfile() throws Exception {
        givenExistingUser();

        mvc.perform(delete("/myProfile")
                        .header("User-Agent", "UserAgent")
                        .secure(true)
                        .with(basicHttpAuthForExistingUser())
                        .with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(validOpenApiResponse());

        verify(userDao).anonymizeUser(eq(EXISTING_USER_ID));
        verify(userDao).addUsernameToBlocklist(EXISTING_USER_NAME);
        verify(authorizationDao).deleteAllByUser(EXISTING_USER_NAME);
    }

}
