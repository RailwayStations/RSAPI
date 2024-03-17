package org.railwaystations.rsapi.adapter.mail

import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.ServerSetupTest
import jakarta.mail.internet.MimeMultipart
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


class SmtpMailerTest {

    @Test
    fun send() {
        val greenMail = GreenMail(ServerSetupTest.SMTP)
        greenMail.setUser("user", "password")
        greenMail.start()
        val sut = SmtpMailer(
            MailerConfig(
                host = "localhost",
                port = greenMail.smtp.port.toString(),
                user = "user",
                passwd = "password",
                from = "info@railway-stations.org",
                ssl = false
            )
        )

        sut.send("to@example.com", "subject", "content")

        val receivedMessages = greenMail.receivedMessages
        assertThat(receivedMessages.size).isEqualTo(1)
        assertThat(receivedMessages[0].from[0].toString()).isEqualTo("info@railway-stations.org")
        assertThat(receivedMessages[0].allRecipients[0].toString()).isEqualTo("to@example.com")
        assertThat(receivedMessages[0].subject).isEqualTo("subject")
        val mp: MimeMultipart = receivedMessages[0].content as MimeMultipart
        assertThat(mp.getBodyPart(0).content).isEqualTo("content")
        greenMail.stop()
    }
}