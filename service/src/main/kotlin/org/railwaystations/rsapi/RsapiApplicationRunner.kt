package org.railwaystations.rsapi

import org.railwaystations.rsapi.core.ports.inbound.GetStatisticUseCase
import org.railwaystations.rsapi.core.ports.outbound.MonitorPort
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class RsapiApplicationRunner(
    private val monitorPort: MonitorPort,
    private val getStatisticUseCase: GetStatisticUseCase,
) : CommandLineRunner {

    override fun run(vararg args: String) {
        monitorPort.sendMessage(getStatisticUseCase.countryStatisticMessage)
    }
}
