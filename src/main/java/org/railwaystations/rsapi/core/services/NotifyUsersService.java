package org.railwaystations.rsapi.core.services;

import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.railwaystations.rsapi.adapter.out.db.InboxDao;
import org.railwaystations.rsapi.adapter.out.db.UserDao;
import org.railwaystations.rsapi.core.model.InboxEntry;
import org.railwaystations.rsapi.core.model.User;
import org.railwaystations.rsapi.core.ports.in.NotifyUsersUseCase;
import org.railwaystations.rsapi.core.ports.out.Mailer;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Service
@Slf4j
public class NotifyUsersService implements NotifyUsersUseCase {

    private final UserDao userDao;
    private final InboxDao inboxDao;
    private final Mailer mailer;

    public NotifyUsersService(UserDao userDao, InboxDao inboxDao, Mailer mailer) {
        super();
        this.userDao = userDao;
        this.inboxDao = inboxDao;
        this.mailer = mailer;
    }

    @Override
    public void notifyUsers() {
        var entries = inboxDao.findInboxEntriesToNotify();
        entries.stream()
                .collect(groupingBy(InboxEntry::getPhotographerId))
                .forEach((userId, entriesForUser) -> userDao.findById(userId).ifPresent(user -> {
                    if (user.getEmail() != null && user.isEmailVerified() && user.isSendNotifications()) {
                        sendEmailNotification(user, entriesForUser);
                    }
                }));
        var ids = entries.stream()
                .map(InboxEntry::getId)
                .collect(Collectors.toList());
        if (!ids.isEmpty()) {
            inboxDao.updateNotified(ids);
        }
    }

    private void sendEmailNotification(@NotNull User user, List<InboxEntry> entriesForUser) {
        var report = new StringBuilder();
        entriesForUser.forEach(entry -> report.append(entry.getId()).append(". ").append(entry.getTitle())
                .append(entry.isProblemReport() ? " (" + entry.getProblemReportType() + ")" : "")
                .append(": ")
                .append(entry.getRejectReason() == null ? "accepted" : "rejected - " + entry.getRejectReason())
                .append("\n"));

        var text = String.format("""
                Hello %1$s,
                                
                thank you for your contributions.
                                
                Cheers
                Your Railway-Stations-Team
                                
                ---
                Hallo %1$s,
                                
                vielen Dank für Deine Beiträge.
                                
                Viele Grüße
                Dein Bahnhofsfoto-Team
                                
                ---------------------------------
                                
                %2$s""", user.getName(), report);
        mailer.send(user.getEmail(), "Railway-Stations.org review result", text);
        log.info("Email notification sent to {}", user.getEmail());
    }

}
