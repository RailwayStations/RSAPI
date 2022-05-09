package org.railwaystations.rsapi.adapter.out.mail;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.railwaystations.rsapi.core.ports.out.Mailer;
import org.springframework.stereotype.Service;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

@Service
@Slf4j
public class SmtpMailer implements Mailer {

    private final MailerConfig config;

    private Session session;

    public SmtpMailer(final MailerConfig config) {
        this.config = config;
        if (StringUtils.isNoneBlank(config.host())) {
            final var properties = System.getProperties();
            properties.setProperty("mail.smtp.host", config.host());
            properties.setProperty("mail.smtp.auth", "true");
            properties.setProperty("mail.smtp.port", config.port());
            properties.setProperty("mail.smtp.ssl.enable", "true");
            properties.setProperty("mail.smtp.timeout", "10000");
            properties.setProperty("mail.smtp.connectiontimeout", "10000");

            session = Session.getInstance(properties, new UsernamePasswordAuthenticator(config.user(), config.passwd()));
        }
    }

    @Override
    public void send(final String to, final String subject, final String text) {
        if (session == null) {
            log.info("Mailer not initialized, can't send mail to {} with subject {} and body {}", to, subject, text);
            return;
        }
        try {
            log.info("Sending mail to {}", to);
            final var message = new MimeMessage(session);
            message.setFrom(new InternetAddress(config.from()));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);

            final var multipart = new MimeMultipart();

            final var textBodyPart = new MimeBodyPart();
            textBodyPart.setText(text);
            multipart.addBodyPart(textBodyPart);

            message.setContent(multipart);
            Transport.send(message);
            log.info("Mail sent");
        } catch (final Exception e) {
            throw new RuntimeException("Unable to send mail", e);
        }
    }

    private static class UsernamePasswordAuthenticator extends Authenticator {
        private final String user;
        private final String passwd;

        private UsernamePasswordAuthenticator(final String user, final String passwd) {
            this.user = user;
            this.passwd = passwd;
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(user, passwd);
        }
    }
}
