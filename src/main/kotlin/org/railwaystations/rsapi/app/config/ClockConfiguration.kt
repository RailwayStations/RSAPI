package org.railwaystations.rsapi.app.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class ClockConfiguration {
    @Bean
    fun createClock(): Clock {
        return Clock.systemDefaultZone()
    }
}
