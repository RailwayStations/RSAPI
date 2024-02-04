package org.railwaystations.rsapi.adapter.monitoring

import org.railwaystations.rsapi.core.ports.Monitor
import java.nio.file.Path

class FakeMonitor : Monitor {
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
