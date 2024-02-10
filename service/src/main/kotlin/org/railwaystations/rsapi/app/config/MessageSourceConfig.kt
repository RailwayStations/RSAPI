package org.railwaystations.rsapi.app.config

import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ResourceBundleMessageSource

@Configuration
class MessageSourceConfig {
    @Bean("messageSource")
    fun messageSource(): MessageSource {
        val messageSource = ResourceBundleMessageSource()
        messageSource.setBasenames("language/messages")
        messageSource.setDefaultEncoding("UTF-8")
        return messageSource
    }
}
