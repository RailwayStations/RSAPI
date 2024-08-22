package org.railwaystations.rsapi

import org.railwaystations.rsapi.core.ports.outbound.MonitorPort
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class RsapiApplicationRunner(
    private val monitorPort: MonitorPort,
) : CommandLineRunner {

    override fun run(vararg args: String) {
        monitorPort.sendMessage("RSAPI startup")
    }
}
