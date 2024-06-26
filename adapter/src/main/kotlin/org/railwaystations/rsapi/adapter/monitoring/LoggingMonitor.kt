package org.railwaystations.rsapi.adapter.monitoring

import org.railwaystations.rsapi.core.ports.outbound.MonitorPort
import org.railwaystations.rsapi.core.utils.Logger
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.nio.file.Path

@Service
@ConditionalOnProperty(prefix = "monitor", name = ["service"], havingValue = "logging")
class LoggingMonitor : MonitorPort {

    private val log by Logger()

    override fun sendMessage(message: String) {
        log.info(message)
    }

    override fun sendMessage(message: String, file: Path?) {
        log.info("$message - $file")
    }
}
