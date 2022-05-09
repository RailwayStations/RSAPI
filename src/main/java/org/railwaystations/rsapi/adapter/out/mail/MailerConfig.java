package org.railwaystations.rsapi.adapter.out.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mailer")
public record MailerConfig(String host, String port, String user, String passwd, String from) { }
