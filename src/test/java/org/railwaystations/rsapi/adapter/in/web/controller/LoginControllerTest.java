package org.railwaystations.rsapi.adapter.in.web.controller;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.railwaystations.rsapi.adapter.in.web.ErrorHandlingControllerAdvice;
import org.railwaystations.rsapi.app.config.MessageSourceConfig;
import org.railwaystations.rsapi.core.model.User;
import org.railwaystations.rsapi.core.ports.in.ManageProfileUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Locale;

import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LoginController.class, properties = {"mailVerificationUrl=EMAIL_VERIFICATION_URL"})
@ContextConfiguration(classes = {
        WebMvcTestApplication.class,
        ErrorHandlingControllerAdvice.class,
        MockMvcTestConfiguration.class,
        MessageSourceConfig.class
})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("mockMvcTest")
class LoginControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    private ManageProfileUseCase manageProfileUseCase;

    @Test
    public void getLogin() throws Exception {
        mvc.perform(get("/login")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("<form class=\"form-signin\" action=\"/login\"")));
    }

    @Test
    public void postLoginResetPasswordSuccess() throws Exception {
        mvc.perform(post("/loginResetPassword")
                        .param("username", "a_user")
                        .with(csrf())
                        .header(HttpHeaders.USER_AGENT, "user_agent"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "/login?reset_password_success&username=a_user"));

        verify(manageProfileUseCase).resetPassword("a_user", "user_agent");
    }

    @Test
    public void postLoginResetPasswordError() throws Exception {
        doThrow(new RuntimeException("test")).when(manageProfileUseCase).resetPassword("a_user", "user_agent");

        mvc.perform(post("/loginResetPassword")
                        .param("username", "a_user")
                        .with(csrf())
                        .header(HttpHeaders.USER_AGENT, "user_agent"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "/login?reset_password_error&username=a_user"));
    }

    @Test
    public void getLoginRegister() throws Exception {
        mvc.perform(get("/loginRegister")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("<form action=\"/loginRegister\"")));
    }

    @Test
    public void postLoginRegister() throws Exception {
        var user = createValidUser().build();

        postLoginRegister(user)
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "/login?register_success&username=a_user"));

        verify(manageProfileUseCase).register(refEq(user), eq("user_agent"));
    }

    @NotNull
    private ResultActions postLoginRegister(String name, String email, String newPassword, String passwordRepeat, String locale) throws Exception {
        return mvc.perform(post("/loginRegister")
                .param("username", name)
                .param("email", email)
                .param("password", newPassword)
                .param("passwordRepeat", passwordRepeat)
                .with(csrf())
                .header(HttpHeaders.USER_AGENT, "user_agent")
                .header(HttpHeaders.ACCEPT_LANGUAGE, locale));
    }

    @NotNull
    private ResultActions postLoginRegister(User user) throws Exception {
        return postLoginRegister(user.getName(), user.getEmail(), user.getNewPassword(), user.getNewPassword(), user.getLocaleLanguageTag());
    }

    private static User.UserBuilder createValidUser() {
        return User.builder()
                .name("a_user")
                .email("email@example.com")
                .newPassword("very_secret")
                .locale(Locale.GERMANY);
    }

    @Test
    public void postLoginRegisterPasswordsDontMatch() throws Exception {
        var user = createValidUser().build();

        postLoginRegister(user.getName(), user.getEmail(), user.getNewPassword(), "something_else", user.getLocaleLanguageTag())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("<div>Passwörter stimmen nicht überein</div>")));

        verify(manageProfileUseCase, never()).register(any(User.class), any(String.class));
    }

    @Test
    public void postLoginRegisterPasswordTooShort() throws Exception {
        var user = createValidUser().newPassword("blah").build();

        postLoginRegister(user)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("<div>Passwort muss mindestens 8 Zeichen lang sein</div>")));

        verify(manageProfileUseCase, never()).register(any(User.class), any(String.class));
    }

    @Test
    public void postLoginRegisterPasswordMissing() throws Exception {
        var user = createValidUser().newPassword(null).build();

        postLoginRegister(user)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("<div>Eingabe erforderlich</div>")));

        verify(manageProfileUseCase, never()).register(any(User.class), any(String.class));
    }

    @Test
    public void postLoginRegisterEmptyName() throws Exception {
        var user = createValidUser().name(null).build();

        postLoginRegister(user)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("<div>Eingabe erforderlich</div>")));

        verify(manageProfileUseCase, never()).register(any(User.class), any(String.class));
    }

    @Test
    public void postLoginRegisterEmptyEmail() throws Exception {
        var user = createValidUser().email(null).build();

        postLoginRegister(user)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("<div>Eingabe erforderlich</div>")));

        verify(manageProfileUseCase, never()).register(any(User.class), any(String.class));
    }

    @Test
    public void postLoginRegisterConflict() throws Exception {
        var user = createValidUser().build();
        doThrow(new ManageProfileUseCase.ProfileConflictException())
                .when(manageProfileUseCase).register(any(User.class), any(String.class));

        postLoginRegister(user)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Benutzername oder E-Mail sind bereits belegt!")));
    }

    @Test
    public void postLoginRegisterGlobalError() throws Exception {
        var user = createValidUser().build();
        doThrow(new RuntimeException())
                .when(manageProfileUseCase).register(any(User.class), any(String.class));

        postLoginRegister(user)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Fehler beim Anlegen des neuen Kontos. Besteht das Problem weiterhin, kontaktiere uns bitte unter info@railway-stations.org.")));
    }
}