package org.railwaystations.rsapi

import org.railwaystations.rsapi.core.ports.GetStatisticUseCase
import org.railwaystations.rsapi.core.ports.Monitor
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class RsapiApplicationRunner(
    private val monitor: Monitor,
    private val getStatisticUseCase: GetStatisticUseCase,
) : CommandLineRunner {

    override fun run(vararg args: String) {
        monitor.sendMessage(getStatisticUseCase.countryStatisticMessage)
    }
}
