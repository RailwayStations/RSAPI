package org.railwaystations.rsapi.adapter.mail

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "mailer")
data class MailerConfig(val host: String, val port: String, val user: String, val passwd: String, val from: String)
