package org.railwaystations.rsapi.adapter.mail

import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import org.apache.commons.lang3.StringUtils
import org.railwaystations.rsapi.core.ports.Mailer
import org.railwaystations.rsapi.utils.Logger
import org.springframework.stereotype.Service

@Service
class SmtpMailer(private val config: MailerConfig) : Mailer {
    private var session: Session? = null
    private val log by Logger()

    init {
        if (StringUtils.isNoneBlank(config.host)) {
            val properties = System.getProperties()
            properties.setProperty("mail.smtp.host", config.host)
            properties.setProperty("mail.smtp.auth", "true")
            properties.setProperty("mail.smtp.port", config.port)
            properties.setProperty("mail.smtp.ssl.enable", "true")
            properties.setProperty("mail.smtp.timeout", "10000")
            properties.setProperty("mail.smtp.connectiontimeout", "10000")

            session = Session.getInstance(properties, UsernamePasswordAuthenticator(config.user, config.passwd))
        }
    }

    override fun send(to: String?, subject: String?, text: String?) {
        if (session == null) {
            log.info(
                "Mailer not initialized, can't send mail to {} with subject {} and body {}",
                to,
                subject,
                text
            )
            return
        }
        try {
            log.info("Sending mail to {}", to)
            val message = MimeMessage(session)
            message.setFrom(InternetAddress(config.from))
            message.addRecipient(Message.RecipientType.TO, InternetAddress(to))
            message.subject = subject

            val multipart = MimeMultipart()

            val textBodyPart = MimeBodyPart()
            textBodyPart.setText(text)
            multipart.addBodyPart(textBodyPart)

            message.setContent(multipart)
            Transport.send(message)
            log.info("Mail sent")
        } catch (e: Exception) {
            throw RuntimeException("Unable to send mail", e)
        }
    }

    private class UsernamePasswordAuthenticator(private val user: String, private val passwd: String) :
        Authenticator() {
        override fun getPasswordAuthentication(): PasswordAuthentication {
            return PasswordAuthentication(user, passwd)
        }
    }
}
