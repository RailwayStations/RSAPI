package org.railwaystations.rsapi.core.ports

interface Mailer {
    fun send(to: String?, subject: String?, text: String?)
}
