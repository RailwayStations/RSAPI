package org.railwaystations.rsapi.adapter.db

import org.springframework.boot.autoconfigure.jooq.DefaultConfigurationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JooqCustomizerConfiguration {

    @Bean
    fun configurationCustomizer() =
        DefaultConfigurationCustomizer {
            it.settings().withExecuteWithOptimisticLocking(true)
        }

}
