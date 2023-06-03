package org.railwaystations.rsapi.app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@Configuration
public class ClockTestConfiguration {

    @Bean
    public Clock createClock() {
        return Clock.fixed(Instant.now(), ZoneId.systemDefault());
    }

}
