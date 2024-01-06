package org.railwaystations.rsapi.core.services

import org.railwaystations.rsapi.adapter.db.InboxDao
import org.railwaystations.rsapi.adapter.db.UserDao
import org.railwaystations.rsapi.core.model.InboxEntry
import org.railwaystations.rsapi.core.model.User
import org.railwaystations.rsapi.core.ports.Mailer
import org.railwaystations.rsapi.core.ports.NotifyUsersUseCase
import org.railwaystations.rsapi.utils.Logger
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service

@Service
class NotifyUsersService(
    private val userDao: UserDao,
    private val inboxDao: InboxDao,
    private val mailer: Mailer,
    private val messageSource: MessageSource
) : NotifyUsersUseCase {

    private val log by Logger()

    override fun notifyUsers() {
        val entries = inboxDao.findInboxEntriesToNotify()
        entries
            .groupBy { it.photographerId }
            .forEach { (userId, entriesForUser) ->
                userDao.findById(userId)?.let { user: User ->
                    if (user.email != null && user.isEmailVerified && user.sendNotifications) {
                        sendEmailNotification(user, entriesForUser)
                    }
                }
            }
        val ids = entries.map(InboxEntry::id)
        if (ids.isNotEmpty()) {
            inboxDao.updateNotified(ids)
        }
    }

    private fun sendEmailNotification(user: User, entriesForUser: List<InboxEntry>) {
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

        val text = messageSource.getMessage("review_mail", arrayOf(user.name, report), user.locale)
        mailer.send(user.email, "Railway-Stations.org review result", text)
        log.info("Email notification sent to {}", user.email)
    }
}
