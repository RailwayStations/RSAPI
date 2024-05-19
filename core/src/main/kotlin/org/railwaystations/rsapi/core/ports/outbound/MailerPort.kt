package org.railwaystations.rsapi.core.ports.outbound

interface MailerPort {
    fun send(to: String, subject: String, text: String)
}