package org.railwaystations.rsapi.core.services;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.railwaystations.rsapi.adapter.out.db.InboxDao;
import org.railwaystations.rsapi.adapter.out.db.UserDao;
import org.railwaystations.rsapi.core.model.InboxEntry;
import org.railwaystations.rsapi.core.model.User;
import org.railwaystations.rsapi.core.ports.out.Mailer;

import java.util.List;
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
    final NotifyUsersService service = new NotifyUsersService(userDao, inboxDao, mailer);

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @ParameterizedTest
    @MethodSource("provideUsersToNotNotify")
    public void doNotSendEmail(final Optional<User> user) {
        when(inboxDao.findInboxEntriesToNotify()).thenReturn(createInboxEntriesToNotify());
        when(userDao.findById(1)).thenReturn(user);
        service.notifyUsers();
        verify(mailer, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    public void sendEmail() {
        when(inboxDao.findInboxEntriesToNotify()).thenReturn(createInboxEntriesToNotify());
        when(userDao.findById(1)).thenReturn(createUser("nickname@example.com", EMAIL_VERIFIED, true));
        service.notifyUsers();
        verify(mailer).send("nickname@example.com", "Railway-Stations.org review result", """
                Hello nickname,

                thank you for your contributions.

                Cheers
                Your Railway-Stations-Team

                ---
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

    private static Optional<User> createUser(final String email, final String emailVerification, final boolean sendNotifications) {
        return Optional.of(new User("nickname", null, null, 1, email, true, false, null, false, emailVerification, sendNotifications));
    }

    @NotNull
    private List<InboxEntry> createInboxEntriesToNotify() {
        return List.of(
                createInboxEntry(1, "1", "Title 1", null),
                createInboxEntry(2, "2", "Title 2", "rejectedReason")
        );
    }

    @NotNull
    private InboxEntry createInboxEntry(final int id, final String stationId, final String title, final String rejectReason) {
        return new InboxEntry(id, "de", stationId, title, null, 1, "nickname", "nickname@example.com", null, null, rejectReason, null, true, null, false, false, null, true, null, false);
    }

}