package org.railwaystations.rsapi.core.services;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.railwaystations.rsapi.adapter.out.db.InboxDao;
import org.railwaystations.rsapi.adapter.out.db.UserDao;
import org.railwaystations.rsapi.app.config.MessageSourceConfig;
import org.railwaystations.rsapi.core.model.InboxEntry;
import org.railwaystations.rsapi.core.model.User;
import org.railwaystations.rsapi.core.ports.out.Mailer;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.railwaystations.rsapi.core.model.User.EMAIL_VERIFIED;

class NotifyUsersServiceTest {

    final UserDao userDao = mock(UserDao.class);
    final InboxDao inboxDao = mock(InboxDao.class);
    final Mailer mailer = mock(Mailer.class);
    final NotifyUsersService service = new NotifyUsersService(userDao, inboxDao, mailer, new MessageSourceConfig().messageSource());

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @ParameterizedTest
    @MethodSource("provideUsersToNotNotify")
    void doNotSendEmail(Optional<User> user) {
        when(inboxDao.findInboxEntriesToNotify()).thenReturn(createInboxEntriesToNotify());
        when(userDao.findById(1)).thenReturn(user);
        service.notifyUsers();
        verify(mailer, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void sendEmail() {
        when(inboxDao.findInboxEntriesToNotify()).thenReturn(createInboxEntriesToNotify());
        when(userDao.findById(1)).thenReturn(createUser("nickname@example.com", EMAIL_VERIFIED, true));
        service.notifyUsers();
        verify(mailer).send("nickname@example.com", "Railway-Stations.org review result", """
                Hallo nickname,

                vielen Dank für Deine Beiträge.

                Viele Grüße
                Dein Bahnhofsfoto-Team

                ---------------------------------

                1. Title 1: accepted
                2. Title 2: rejected - rejectedReason
                """);
    }

    @NotNull
    private static Stream<Arguments> provideUsersToNotNotify() {
        return Stream.of(
                Arguments.of(Optional.empty()),
                Arguments.of(createUser(null, null, true)),
                Arguments.of(createUser("nickname@example.com", null, true)),
                Arguments.of(createUser("nickname@example.com", EMAIL_VERIFIED, false))
        );
    }

    private static Optional<User> createUser(String email, String emailVerification, boolean sendNotifications) {
        return Optional.of(User.builder().name("nickname").id(1).email(email).ownPhotos(true).anonymous(false).admin(false).emailVerification(emailVerification).sendNotifications(sendNotifications).locale(Locale.GERMAN).build());
    }

    @NotNull
    private List<InboxEntry> createInboxEntriesToNotify() {
        return List.of(
                createInboxEntry(1, "1", "Title 1", null),
                createInboxEntry(2, "2", "Title 2", "rejectedReason")
        );
    }

    @NotNull
    private InboxEntry createInboxEntry(int id, String stationId, String title, String rejectReason) {
        return InboxEntry.builder()
                .id(id)
                .countryCode("de")
                .stationId(stationId)
                .title(title)
                .photographerId(1)
                .photographerNickname("nickname")
                .photographerEmail("nickname@example.com")
                .rejectReason(rejectReason)
                .build();
    }

}