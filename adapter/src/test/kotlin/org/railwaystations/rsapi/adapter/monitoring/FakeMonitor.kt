package org.railwaystations.rsapi.adapter.monitoring

import org.railwaystations.rsapi.core.ports.outbound.MonitorPort
import java.nio.file.Path

class FakeMonitor : MonitorPort {
    private val messages = mutableListOf<String>()

    override fun sendMessage(message: String) {
        messages.add(message)
    }

    override fun sendMessage(message: String, file: Path?) {
        messages.add(message)
    }

    fun getMessages(): List<String> {
        return messages
    }

    fun reset() {
        messages.clear()
    }
}
