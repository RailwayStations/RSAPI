package org.railwaystations.rsapi.adapter.mail;

import org.apache.commons.lang3.StringUtils;
import org.railwaystations.rsapi.domain.port.out.Mailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Properties;

@Service
public class SmtpMailer implements Mailer {

    private static final Logger LOG = LoggerFactory.getLogger(SmtpMailer.class);
    private final MailerConfig config;

    private Session session;

    public SmtpMailer(final MailerConfig config) {
        this.config = config;
        if (StringUtils.isNoneBlank(config.getHost())) {
            final Properties properties = System.getProperties();
            properties.setProperty("mail.smtp.host", config.getHost());
            properties.setProperty("mail.smtp.auth", "true");
            properties.setProperty("mail.smtp.port", config.getPort());
            properties.setProperty("mail.smtp.ssl.enable", "true");
            properties.setProperty("mail.smtp.timeout", "10000");
            properties.setProperty("mail.smtp.connectiontimeout", "10000");

            session = Session.getInstance(properties, new UsernamePasswordAuthenticator(config.getUser(), config.getPasswd()));
        }
    }

    @Override
    public void send(final String to, final String subject, final String text) {
        if (session == null) {
            LOG.info("Mailer not initialized, can't send mail to {} with subject {} and body {}", to, subject, text);
            return;
        }
        try {
            LOG.info("Sending mail to {}", to);
            final MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(config.getFrom()));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);

            final Multipart multipart = new MimeMultipart();

            final MimeBodyPart textBodyPart = new MimeBodyPart();
            textBodyPart.setText(text);
            multipart.addBodyPart(textBodyPart);

            message.setContent(multipart);
            Transport.send(message);
            LOG.info("Mail sent");
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
