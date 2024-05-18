package org.railwaystations.rsapi.core.services

import org.railwaystations.rsapi.core.model.InboxEntry
import org.railwaystations.rsapi.core.model.User
import org.railwaystations.rsapi.core.ports.InboxPort
import org.railwaystations.rsapi.core.ports.Mailer
import org.railwaystations.rsapi.core.ports.NotifyUsersUseCase
import org.railwaystations.rsapi.core.ports.UserPort
import org.railwaystations.rsapi.core.utils.Logger
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import java.util.*

@Service
class NotifyUsersService(
    private val userPort: UserPort,
    private val inboxPort: InboxPort,
    private val mailer: Mailer,
    private val messageSource: MessageSource
) : NotifyUsersUseCase {

    private val log by Logger()

    override fun notifyUsers() {
        val entries = inboxPort.findInboxEntriesToNotify()
        entries
            .groupBy { it.photographerId }
            .forEach { (userId, entriesForUser) ->
                userPort.findById(userId)?.let { user: User ->
                    if (user.email != null && user.isEmailVerified && user.sendNotifications) {
                        sendEmailNotification(entriesForUser, user.email, user.name, user.locale)
                    }
                }
            }
        val ids = entries.map(InboxEntry::id)
        if (ids.isNotEmpty()) {
            inboxPort.updateNotified(ids)
        }
    }

    private fun sendEmailNotification(
        entriesForUser: List<InboxEntry>, email: String, username: String, locale: Locale
    ) {
        val report = buildString {
            entriesForUser.forEach { entry ->
                append("${entry.id}. ${entry.title}")
                if (entry.isProblemReport) {
                    append(" (${entry.problemReportType})")
                }
                append(": ")
                if (entry.rejectReason == null) {
                    append("accepted")
                } else {
                    append("rejected - ${entry.rejectReason}")
                }
                append("\n")
            }
        }

        val text = messageSource.getMessage("review_mail", arrayOf(username, report), locale)
        mailer.send(email, "Railway-Stations.org review result", text)
        log.info("Email notification sent to {}", email)
    }
}
