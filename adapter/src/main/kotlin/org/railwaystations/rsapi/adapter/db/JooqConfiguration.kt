package org.railwaystations.rsapi.adapter.db

import org.springframework.boot.autoconfigure.jooq.DefaultConfigurationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JooqConfiguration {

    @Bean
    fun jooqCustomizer() =
        DefaultConfigurationCustomizer { c ->
            c.settings()
                .withExecuteWithOptimisticLocking(true)
        }

}
