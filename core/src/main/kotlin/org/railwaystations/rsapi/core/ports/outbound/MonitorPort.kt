package org.railwaystations.rsapi.core.ports.outbound

import java.nio.file.Path

interface MonitorPort {
    fun sendMessage(message: String)
    fun sendMessage(message: String, file: Path?)
}