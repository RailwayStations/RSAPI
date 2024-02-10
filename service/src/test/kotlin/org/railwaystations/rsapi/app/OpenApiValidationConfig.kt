package org.railwaystations.rsapi.app

import com.atlassian.oai.validator.springmvc.OpenApiValidationFilter
import com.atlassian.oai.validator.springmvc.OpenApiValidationInterceptor
import jakarta.servlet.Filter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import org.springframework.core.io.support.EncodedResource
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.io.IOException
import java.nio.charset.StandardCharsets


@Configuration
class OpenApiValidationConfig {
    @Bean
    fun validationFilter(): Filter {
        return OpenApiValidationFilter(
            true,  // enable request validation
            true // enable response validation
        )
    }

    @Bean
    @Throws(IOException::class)
    fun addOpenApiValidationInterceptor(@Value("classpath:static/openapi.yaml") openApiSpecification: Resource?): WebMvcConfigurer {
        val specResource = EncodedResource(
            openApiSpecification!!, StandardCharsets.UTF_8
        )
        val openApiValidationInterceptor = OpenApiValidationInterceptor(specResource)
        return object : WebMvcConfigurer {
            override fun addInterceptors(registry: InterceptorRegistry) {
                registry.addInterceptor(openApiValidationInterceptor)
            }
        }
    }
}