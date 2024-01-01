package org.railwaystations.rsapi.adapter.in.web.controller;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.railwaystations.rsapi.adapter.in.web.ErrorHandlingControllerAdvice;
import org.railwaystations.rsapi.adapter.out.db.OAuth2AuthorizationDao;
import org.railwaystations.rsapi.adapter.out.db.UserDao;
import org.railwaystations.rsapi.app.auth.LazySodiumPasswordEncoder;
import org.railwaystations.rsapi.app.auth.RSAuthenticationProvider;
import org.railwaystations.rsapi.app.auth.RSUserDetailsService;
import org.railwaystations.rsapi.app.auth.WebSecurityConfig;
import org.railwaystations.rsapi.app.config.MessageSourceConfig;
import org.railwaystations.rsapi.core.model.License;
import org.railwaystations.rsapi.core.model.User;
import org.railwaystations.rsapi.core.ports.in.ManageProfileUseCase;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
@Import({ProfileService.class, RSUserDetailsService.class, RSAuthenticationProvider.class, LazySodiumPasswordEncoder.class, MessageSourceConfig.class})
@ActiveProfiles("mockMvcTest")
public class ProfileControllerTest {

    public static final String USER_NAME = "existing";
    public static final String USER_EMAIL = "existing@example.com";
    public static final int EXISTING_USER_ID = 42;
    public static final String USER_AGENT = "UserAgent";
    @Autowired
    private MockMvc mvc;

    @MockBean
    private ProfileService profileService;

    @MockBean
    private UserDao userDao;

    @MockBean
    private OAuth2AuthorizationDao authorizationDao;

    @Test
    void getMyProfile() throws Exception {
        givenExistingUser();

        mvc.perform(get("/myProfile")
                        .header("User-Agent", "UserAgent")
                        .contentType("application/json")
                        .secure(true)
                        .with(basicHttpAuthForExistingUser())
                        .with(csrf()))
                .andExpect(validOpenApiResponse())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value(USER_NAME));
    }

    private User givenExistingUser() {
        var key = "246172676F6E32696424763D3139246D3D36353533362C743D322C703D3124426D4F637165757646794E44754132726B566A6A3177246A7568362F6E6C2F49437A4B475570446E6B674171754A304F7A486A62694F587442542F2B62584D49476300000000000000000000000000000000000000000000000000000000000000";
        var user = createUser()
                .id(EXISTING_USER_ID)
                .key(key)
                .build();
        when(userDao.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userDao.findByNormalizedName(user.getName())).thenReturn(Optional.of(user));
        return user;
    }

    private static User.UserBuilder createUser() {
        return User.builder()
                .name(USER_NAME)
                .license(License.CC0_10)
                .email(USER_EMAIL)
                .ownPhotos(true)
                .anonymous(false)
                .admin(false)
                .sendNotifications(true);
    }

    @NotNull
    private RequestPostProcessor basicHttpAuthForExistingUser() {
        return httpBasic(USER_EMAIL, "y89zFqkL6hro");
    }

    @NotNull
    private ResultActions postChangePassword(String newPasswordBody) throws Exception {
        MockHttpServletRequestBuilder action = post("/changePassword")
                .header("User-Agent", "UserAgent")
                .secure(true)
                .with(basicHttpAuthForExistingUser())
                .with(csrf());

        if (newPasswordBody != null) {
            action = action.contentType("application/json")
                    .content(newPasswordBody);

        }

        return mvc.perform(action).andExpect(validOpenApiResponse());
    }

    @Test
    void changePasswordTooShortBody() throws Exception {
        givenExistingUser();
        doThrow(new IllegalArgumentException())
                .when(profileService).changePassword(any(User.class), eq("secret"));

        postChangePassword("{\"newPassword\": \"secret\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePasswordBody() throws Exception {
        var user = givenExistingUser();

        postChangePassword("{\"newPassword\": \"secretlong\"}")
                .andExpect(status().isOk());

        verify(profileService).changePassword(user, "secretlong");
    }

    @Test
    void updateMyProfile() throws Exception {
        var existingUser = givenExistingUser();
        var newProfileJson = """
                    { "nickname": "new_name", "email": "%s", "link": "http://twitter.com/", "license": "CC0", "anonymous": true, "sendNotifications": true, "photoOwner": true }
                """.formatted(USER_EMAIL);

        postMyProfileWithOpenApiValidation(newProfileJson).andExpect(status().isOk());

        var user = createUser()
                .id(EXISTING_USER_ID)
                .name("new_name")
                .build();
        verify(profileService).updateProfile(existingUser, user, USER_AGENT);
    }

    @NotNull
    private ResultActions postMyProfileWithOpenApiValidation(String newProfileJson) throws Exception {
        return postMyProfile(newProfileJson)
                .andExpect(validOpenApiResponse());
    }

    @NotNull
    private ResultActions postMyProfile(String newProfileJson) throws Exception {
        return mvc.perform(post("/myProfile")
                .header("User-Agent", USER_AGENT)
                .contentType("application/json")
                .content(newProfileJson)
                .secure(true)
                .with(basicHttpAuthForExistingUser())
                .with(csrf()));
    }

    @Test
    void updateMyProfileConflict() throws Exception {
        doThrow(new ManageProfileUseCase.ProfileConflictException())
                .when(profileService).updateProfile(any(User.class), any(User.class), anyString());
        givenExistingUser();
        var newProfileJson = """
                    { "nickname": "new_name", "email": "%s", "link": "http://twitter.com/", "license": "CC0", "anonymous": true, "sendNotifications": true, "photoOwner": true }
                """.formatted(USER_EMAIL);

        postMyProfileWithOpenApiValidation(newProfileJson).andExpect(status().isConflict());
    }

    @Test
    void updateMyProfileNameTooLong() throws Exception {
        var newProfileJson = """
                    { "nickname": "A very long name with a lot of extra words to overfill the database column", "email": "%s", "link": "http://twitter.com/", "license": "CC0", "anonymous": true, "sendNotifications": true, "photoOwner": true }
                """.formatted(USER_EMAIL);

        postMyProfile(newProfileJson).andExpect(status().isBadRequest());

        verify(profileService, never()).updateProfile(any(User.class), any(User.class), anyString());
    }

    @Test
    void updateMyProfileNewMail() throws Exception {
        var existingUser = givenExistingUser();
        var newProfileJson = """
                    { "nickname": "%s", "email": "newname@example.com", "link": "http://twitter.com/", "license": "CC0", "anonymous": true, "sendNotifications": true, "photoOwner": true }
                """.formatted(USER_NAME);

        postMyProfileWithOpenApiValidation(newProfileJson).andExpect(status().isOk());

        var user = createUser()
                .id(EXISTING_USER_ID)
                .email("newname@example.com")
                .build();
        verify(profileService).updateProfile(existingUser, user, USER_AGENT);
    }

    @Test
    void verifyEmailSuccess() throws Exception {
        var token = "verification";
        var user = createUser()
                .id(EXISTING_USER_ID)
                .emailVerification(token)
                .build();
        when(profileService.emailVerification(token)).thenReturn(Optional.of(user));

        getEmailVerification(token)
                .andExpect(status().isOk());
    }

    @NotNull
    private ResultActions getEmailVerification(String token) throws Exception {
        return mvc.perform(get("/emailVerification/" + token)
                        .header("User-Agent", "UserAgent")
                        .with(csrf()))
                .andExpect(validOpenApiResponse());
    }

    @Test
    void verifyEmailFailed() throws Exception {
        var token = "verification";
        var user = createUser()
                .id(EXISTING_USER_ID)
                .emailVerification(token)
                .build();
        when(profileService.emailVerification(token)).thenReturn(Optional.of(user));

        getEmailVerification("wrong_token").andExpect(status().isNotFound());

    }

    @Test
    void resendEmailVerification() throws Exception {
        var user = givenExistingUser();
        mvc.perform(post("/resendEmailVerification")
                        .header("User-Agent", "UserAgent")
                        .secure(true)
                        .with(basicHttpAuthForExistingUser())
                        .with(csrf()))
                .andExpect(validOpenApiResponse())
                .andExpect(status().isOk());

        verify(profileService).resendEmailVerification(user);
    }

    @Test
    void deleteMyProfile() throws Exception {
        var user = givenExistingUser();

        mvc.perform(delete("/myProfile")
                        .header("User-Agent", "UserAgent")
                        .secure(true)
                        .with(basicHttpAuthForExistingUser())
                        .with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(validOpenApiResponse());

        verify(profileService).deleteProfile(user, USER_AGENT);
    }

}
