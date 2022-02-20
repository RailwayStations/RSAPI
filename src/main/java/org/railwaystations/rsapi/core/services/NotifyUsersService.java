package org.railwaystations.rsapi.core.services;

import org.railwaystations.rsapi.adapter.db.InboxDao;
import org.railwaystations.rsapi.adapter.db.UserDao;
import org.railwaystations.rsapi.core.model.InboxEntry;
import org.railwaystations.rsapi.core.model.User;
import org.railwaystations.rsapi.core.ports.Mailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Service
@SuppressWarnings("PMD.BeanMembersShouldSerialize")
public class NotifyUsersService {

    private static final Logger LOG = LoggerFactory.getLogger(NotifyUsersService.class);

    private final UserDao userDao;
    private final InboxDao inboxDao;
    private final Mailer mailer;

    public NotifyUsersService(final UserDao userDao, final InboxDao inboxDao, final Mailer mailer) {
        super();
        this.userDao = userDao;
        this.inboxDao = inboxDao;
        this.mailer = mailer;
    }

    public void notifyUsers() {
        final List<InboxEntry> entries = inboxDao.findInboxEntriesToNotify();
        entries.stream()
                .collect(groupingBy(InboxEntry::getPhotographerId))
                .forEach((userId, entriesForUser) -> userDao.findById(userId).ifPresent(user -> {
                    if (user.getEmail() != null && user.isEmailVerified() && user.isSendNotifications()) {
                        sendEmailNotification(user, entriesForUser);
                    }
                }));
        final List<Integer> ids = entries.stream()
                .map(InboxEntry::getId)
                .collect(Collectors.toList());
        if (!ids.isEmpty()) {
            inboxDao.updateNotified(ids);
        }
    }

    private void sendEmailNotification(@NotNull final User user, final List<InboxEntry> entriesForUser) {
        final StringBuilder report = new StringBuilder();
        entriesForUser.forEach(entry -> report.append(entry.getId()).append(". ").append(entry.getTitle())
                .append(entry.isProblemReport() ? " (" + entry.getProblemReportType() + ")" : "")
                .append(": ")
                .append(entry.getRejectReason() == null ? "accepted" : "rejected - " + entry.getRejectReason())
                .append("\n"));

        final String text = String.format("""
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
                
                %2$s""" , user.getName(), report);
        mailer.send(user.getEmail(), "Railway-Stations.org review result", text);
        LOG.info("Email notification sent to {}", user.getEmail());
    }

}
