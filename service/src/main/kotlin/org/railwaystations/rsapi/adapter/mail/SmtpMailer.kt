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
    private lateinit var session: Session
    private val log by Logger()

    init {
        if (StringUtils.isNoneBlank(config.host)) {
            val properties = System.getProperties().apply {
                setProperty("mail.smtp.host", config.host)
                setProperty("mail.smtp.auth", "true")
                setProperty("mail.smtp.port", config.port)
                setProperty("mail.smtp.ssl.enable", config.ssl.toString())
                setProperty("mail.smtp.timeout", "10000")
                setProperty("mail.smtp.connectiontimeout", "10000")
            }

            session = Session.getInstance(properties, UsernamePasswordAuthenticator(config.user, config.passwd))
        }
    }

    override fun send(to: String, subject: String, text: String) {
        try {
            log.info("Sending mail to {}", to)

            val multipart = MimeMultipart().apply {
                val textBodyPart = MimeBodyPart()
                textBodyPart.setText(text)
                addBodyPart(textBodyPart)
            }

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(config.from))
                addRecipient(Message.RecipientType.TO, InternetAddress(to))
                setSubject(subject)
                setContent(multipart)
            }

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
