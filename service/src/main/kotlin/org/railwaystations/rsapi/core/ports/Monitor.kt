package org.railwaystations.rsapi.core.ports

import java.nio.file.Path

interface Monitor {
    fun sendMessage(message: String)
    fun sendMessage(message: String, file: Path?)
}
