package org.railwaystations.rsapi.adapter.in.web.controller;

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

import java.util.Optional;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
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

    private static final String EMAIL_VERIFICATION_URL = "EMAIL_VERIFICATION_URL";

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
        final var userProfileJson = """
                    { "nickname": "nickname", "link": "https://link@example.com", "license": "CC0", "anonymous": false, "sendNotifications": true, "photoOwner": true }
                """;
        mvc.perform(post("/registration")
                        .header("User-Agent", "UserAgent")
                        .contentType("application/json")
                        .content(userProfileJson)
                        .with(csrf()))
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testRegisterNewUser() throws Exception {
        final var userProfileJson = """
                    { "nickname": "nickname", "email": "nickname@example.com", "link": "https://link@example.com", "license": "CC0", "anonymous": false, "sendNotifications": true, "photoOwner": true }
                """;
        mvc.perform(post("/registration")
                        .header("User-Agent", "UserAgent")
                        .contentType("application/json")
                        .content(userProfileJson)
                        .with(csrf()))
                .andExpect(openApi().isValid("static/openapi.yaml"))
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
        final var userProfileJson = """
                    { "nickname": "nickname", "email": "nickname@example.com", "link": "https://link@example.com", "license": "CC0", "anonymous": false, "sendNotifications": true, "photoOwner": true, "newPassword": "verySecretPassword" }
                """;
        mvc.perform(post("/registration")
                        .header("User-Agent", "UserAgent")
                        .contentType("application/json")
                        .content(userProfileJson)
                        .with(csrf()))
                .andExpect(openApi().isValid("static/openapi.yaml"))
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
        final var userProfileJson = """
                    { "nickname": "nickname", "email": "nickname@example.com", "link": "https://link@example.com", "license": "CC0", "anonymous": true, "sendNotifications": true, "photoOwner": true }
                """;
        mvc.perform(post("/registration")
                        .header("User-Agent", "UserAgent")
                        .contentType("application/json")
                        .content(userProfileJson)
                        .with(csrf()))
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andExpect(status().isAccepted());

        assertThat(monitor.getMessages().get(0), equalTo("New registration{nickname='nickname', email='nickname@example.com', license='CC0 1.0 Universell (CC0 1.0)', photoOwner=true, link='https://link@example.com', anonymous=true}\nvia UserAgent"));
    }

    @Test
    public void testRegisterNewUserNameTaken() throws Exception {
        when(userDao.findByNormalizedName("existing")).thenReturn(Optional.of(new User("existing", "existing@example.com", "CC0", true, "https://link@example.com", false, null, true)));
        final var userProfileJson = """
                    { "nickname": "existing", "email": "other@example.com", "link": "https://link@example.com", "license": "CC0", "anonymous": false, "sendNotifications": true, "photoOwner": true }
                """;
        mvc.perform(post("/registration")
                        .header("User-Agent", "UserAgent")
                        .contentType("application/json")
                        .content(userProfileJson)
                        .with(csrf()))
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andExpect(status().isConflict());
    }

    @Test
    public void testRegisterExistingUserEmailTaken() throws Exception {
        final var user = new User("existing", "existing@example.com", "CC0", true, "https://link@example.com", false, null, true);
        when(userDao.findByNormalizedName(user.getName())).thenReturn(Optional.of(user));
        when(userDao.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        final var userProfileJson = """
                    { "nickname": "existing", "email": "existing@example.com", "link": "https://link@example.com", "license": "CC0", "anonymous": false, "sendNotifications": true, "photoOwner": true }
                """;
        mvc.perform(post("/registration")
                        .header("User-Agent", "UserAgent")
                        .contentType("application/json")
                        .content(userProfileJson)
                        .with(csrf()))
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andExpect(status().isConflict());

        assertThat(monitor.getMessages().get(0), equalTo("Registration for user 'existing' with eMail 'existing@example.com' failed, eMail is already taken\nvia UserAgent"));
    }

    @Test
    public void testRegisterExistingUserNameTaken() throws Exception {
        when(userDao.findByNormalizedName("existing")).thenReturn(Optional.of(new User("existing", "other@example.com", "CC0", true, "https://link@example.com", false, null, true)));
        final var userProfileJson = """
                    { "nickname": "existing", "email": "existing@example.com", "link": "https://link@example.com", "license": "CC0", "anonymous": false, "sendNotifications": true, "photoOwner": true }
                """;
        mvc.perform(post("/registration")
                        .header("User-Agent", "UserAgent")
                        .contentType("application/json")
                        .content(userProfileJson)
                        .with(csrf()))
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andExpect(status().isConflict());

        assertThat(monitor.getMessages().get(0), equalTo("Registration for user 'existing' with eMail 'existing@example.com' failed, name is already taken by different eMail 'other@example.com'\nvia UserAgent"));
    }

    @Test
    public void testRegisterExistingUserEmptyName() throws Exception {
        when(userDao.findByNormalizedName("existing")).thenReturn(Optional.of(new User("existing", "other@example.com", "CC0", true, "https://link@example.com", false, null, true)));
        final var userProfileJson = """
                    { "nickname": "", "email": "existing@example.com", "link": "https://link@example.com", "license": "CC0", "anonymous": false, "sendNotifications": true, "photoOwner": true }
                """;
        mvc.perform(post("/registration")
                        .header("User-Agent", "UserAgent")
                        .contentType("application/json")
                        .content(userProfileJson)
                        .with(csrf()))
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetMyProfile() throws Exception {
        whenFindAuthenticatedUserByEmail();

        mvc.perform(get("/myProfile")
                        .header("User-Agent", "UserAgent")
                        .contentType("application/json")
                        .secure(true)
                        .with(httpBasic("existing@example.com", "y89zFqkL6hro"))
                        .with(csrf()))
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("existing"));
    }

    private void whenFindAuthenticatedUserByEmail() {
        final var key = "246172676F6E32696424763D3139246D3D36353533362C743D322C703D3124426D4F637165757646794E44754132726B566A6A3177246A7568362F6E6C2F49437A4B475570446E6B674171754A304F7A486A62694F587442542F2B62584D49476300000000000000000000000000000000000000000000000000000000000000";
        final var user = new User("existing", null, "CC0", 42, "existing@example.com", true, false, key, false, null, true);
        when(userDao.findByEmail("existing@example.com")).thenReturn(Optional.of(user));
    }

    @Test
    public void testChangePasswordTooShortHeader() throws Exception {
        whenFindAuthenticatedUserByEmail();

        mvc.perform(post("/changePassword")
                        .header("User-Agent", "UserAgent")
                        .header("New-Password", "secret")
                        .contentType("application/json")
                        .secure(true)
                        .with(httpBasic("existing@example.com", "y89zFqkL6hro"))
                        .with(csrf()))
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andExpect(status().isBadRequest());

        verify(userDao, never()).updateCredentials(anyInt(), anyString());
    }

    @Test
    public void testChangePasswordTooShortBody() throws Exception {
        whenFindAuthenticatedUserByEmail();

        mvc.perform(post("/changePassword")
                        .header("User-Agent", "UserAgent")
                        .contentType("application/json")
                        .content("{\"newPassword\": \"secret\"}")
                        .secure(true)
                        .with(httpBasic("existing@example.com", "y89zFqkL6hro"))
                        .with(csrf()))
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andExpect(status().isBadRequest());

        verify(userDao, never()).updateCredentials(anyInt(), anyString());
        verify(userDao, never()).updateCredentials(anyInt(), anyString());
    }

    @Test
    public void testChangePasswordHeader() throws Exception {
        whenFindAuthenticatedUserByEmail();

        mvc.perform(post("/changePassword")
                        .header("User-Agent", "UserAgent")
                        .header("New-Password", "secretlong")
                        .contentType("application/json")
                        .secure(true)
                        .with(httpBasic("existing@example.com", "y89zFqkL6hro"))
                        .with(csrf()))
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andExpect(status().isOk());

        final var idCaptor = ArgumentCaptor.forClass(Integer.class);
        final var keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(userDao).updateCredentials(idCaptor.capture(), keyCaptor.capture());

        assertThat(idCaptor.getValue(), equalTo(42));
        assertThat(new LazySodiumPasswordEncoder().matches("secretlong", keyCaptor.getValue()), is(true));
    }

    @Test
    public void testChangePasswordBody() throws Exception {
        whenFindAuthenticatedUserByEmail();

        mvc.perform(post("/changePassword")
                        .header("User-Agent", "UserAgent")
                        .contentType("application/json")
                        .content("{\"newPassword\": \"secretlong\"}")
                        .secure(true)
                        .with(httpBasic("existing@example.com", "y89zFqkL6hro"))
                        .with(csrf()))
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andExpect(status().isOk());

        final var idCaptor = ArgumentCaptor.forClass(Integer.class);
        final var keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(userDao).updateCredentials(idCaptor.capture(), keyCaptor.capture());

        assertThat(idCaptor.getValue(), equalTo(42));
        assertThat(new LazySodiumPasswordEncoder().matches("secretlong", keyCaptor.getValue()), is(true));
    }

    @Test
    public void testChangePasswordHeaderAndBody() throws Exception {
        whenFindAuthenticatedUserByEmail();

        mvc.perform(post("/changePassword")
                        .header("User-Agent", "UserAgent")
                        .header("New-Password", "secretheader")
                        .contentType("application/json")
                        .content("{\"newPassword\": \"secretbody\"}")
                        .secure(true)
                        .with(httpBasic("existing@example.com", "y89zFqkL6hro"))
                        .with(csrf()))
                .andExpect(openApi().isValid("static/openapi.yaml"))
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
        whenFindAuthenticatedUserByEmail();
        final var newProfileJson = """
                    { "nickname": "new_name", "email": "existing@example.com", "link": "http://twitter.com/", "license": "CC0", "anonymous": true, "sendNotifications": true, "photoOwner": true }
                """;

        mvc.perform(post("/myProfile")
                        .header("User-Agent", "UserAgent")
                        .contentType("application/json")
                        .content(newProfileJson)
                        .secure(true)
                        .with(httpBasic("existing@example.com", "y89zFqkL6hro"))
                        .with(csrf()))
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andExpect(status().isOk());

        verify(userDao).update(new User("new_name", "existing@example.com", "CC0", true, "http://twitter.com/", true, null, true));
    }

    @Test
    public void testUpdateMyProfileConflict() throws Exception {
        when(userDao.findByNormalizedName("newname")).thenReturn(Optional.of(new User("@New name", "newname@example.com", null, true, null, false, null, true)));
        whenFindAuthenticatedUserByEmail();
        final var newProfileJson = """
                    { "nickname": "new_name", "email": "existing@example.com", "link": "http://twitter.com/", "license": "CC0", "anonymous": true, "sendNotifications": true, "photoOwner": true }
                """;

        mvc.perform(post("/myProfile")
                        .header("User-Agent", "UserAgent")
                        .contentType("application/json")
                        .content(newProfileJson)
                        .secure(true)
                        .with(httpBasic("existing@example.com", "y89zFqkL6hro"))
                        .with(csrf()))
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andExpect(status().isConflict());

        verify(userDao, never()).update(any(User.class));
    }

    @Test
    public void testUpdateMyProfileNewMail() throws Exception {
        when(userDao.findByEmail("newname@example.com")).thenReturn(Optional.empty());
        whenFindAuthenticatedUserByEmail();
        final var newProfileJson = """
                    { "nickname": "existing", "email": "newname@example.com", "link": "http://twitter.com/", "license": "CC0", "anonymous": true, "sendNotifications": true, "photoOwner": true }
                """;

        mvc.perform(post("/myProfile")
                        .header("User-Agent", "UserAgent")
                        .contentType("application/json")
                        .content(newProfileJson)
                        .secure(true)
                        .with(httpBasic("existing@example.com", "y89zFqkL6hro"))
                        .with(csrf()))
                .andExpect(openApi().isValid("static/openapi.yaml"))
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
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andExpect(status().isAccepted());

        assertThat(monitor.getMessages().get(0), equalTo("Reset Password for 'existing', email='existing@example.com'"));
    }

    @Test
    public void testNewUploadTokenViaName() throws Exception {
        final var user = new User("existing", "existing@example.com", "CC0", true, "https://link@example.com", false, null, true);
        when(userDao.findByNormalizedName(user.getName())).thenReturn(Optional.of(user));
        when(userDao.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        mvc.perform(post("/newUploadToken")
                        .header("User-Agent", "UserAgent")
                        .header("Email", "existing")
                        .with(csrf()))
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andExpect(status().isAccepted());

        assertThat(monitor.getMessages().get(0), equalTo("Reset Password for 'existing', email='existing@example.com'"));
    }

    @Test
    public void testNewUploadTokenNotFound() throws Exception {
        mvc.perform(post("/newUploadToken")
                        .header("User-Agent", "UserAgent")
                        .header("Email", "doesnt-exist")
                        .with(csrf()))
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testNewUploadTokenEmailMissing() throws Exception {
        final var user = new User("existing", "", "CC0", true, "https://link@example.com", false, null, true);
        when(userDao.findByNormalizedName(user.getName())).thenReturn(Optional.of(user));
        when(userDao.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        mvc.perform(post("/newUploadToken")
                        .header("User-Agent", "UserAgent")
                        .header("Email", "existing")
                        .with(csrf()))
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testVerifyEmailSuccess() throws Exception {
        final var token = "verification";
        final var emailVerification = User.EMAIL_VERIFICATION_TOKEN + token;
        final var user = new User("existing","https://link@example.com", "CC0", 42, "existing@example.com", true, false, null, false, emailVerification, true);
        when(userDao.findByEmailVerification(emailVerification)).thenReturn(Optional.of(user));
        mvc.perform(get("/emailVerification/" + token)
                        .header("User-Agent", "UserAgent")
                        .with(csrf()))
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andExpect(status().isOk());

        assertThat(monitor.getMessages().get(0), equalTo("Email verified {nickname='existing', email='existing@example.com'}"));
        verify(userDao).updateEmailVerification(42, User.EMAIL_VERIFIED);
    }

    @Test
    public void testVerifyEmailFailed() throws Exception {
        final var token = "verification";
        final var emailVerification = User.EMAIL_VERIFICATION_TOKEN + token;
        final var user = new User("existing","https://link@example.com", "CC0", 42, "existing@example.com", true, false, null, false, emailVerification, true);
        when(userDao.findByEmailVerification(emailVerification)).thenReturn(Optional.of(user));

        mvc.perform(get("/emailVerification/wrong_token")
                        .header("User-Agent", "UserAgent")
                        .with(csrf()))
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andExpect(status().isNotFound());

        assertThat(monitor.getMessages().isEmpty(), equalTo(true));
        verify(userDao, never()).updateEmailVerification(42, User.EMAIL_VERIFIED);
    }

    @Test
    public void testResendEmailVerification() throws Exception {
        whenFindAuthenticatedUserByEmail();
        mvc.perform(post("/resendEmailVerification")
                        .header("User-Agent", "UserAgent")
                        .secure(true)
                        .with(httpBasic("existing@example.com", "y89zFqkL6hro"))
                        .with(csrf()))
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andExpect(status().isOk());

        assertVerificationEmail();
        verify(userDao).updateEmailVerification(eq(42), startsWith(User.EMAIL_VERIFICATION_TOKEN));
    }

}
