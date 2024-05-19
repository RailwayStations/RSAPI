package org.railwaystations.rsapi.app

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@Configuration
class ClockTestConfiguration {
    @Bean
    fun createClock(): Clock {
        return Clock.fixed(Instant.now(), ZoneId.systemDefault())
    }
}
