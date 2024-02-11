package org.railwaystations.rsapi.app.config

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.ResourceHttpMessageConverter
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@EnableWebMvc
@Configuration
class WebConfig : WebMvcConfigurer {
    override fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>?>) {
        val builder = Jackson2ObjectMapperBuilder()
        builder.serializationInclusion(JsonInclude.Include.NON_NULL)
        converters.add(MappingJackson2HttpMessageConverter(builder.build()))
        converters.add(resourceHttpMessageConverter())
    }

    override fun addCorsMappings(registry: CorsRegistry) {
        registry
            .addMapping("/**")
            .allowedHeaders(
                "X-Requested-With",
                "Content-Type",
                "Accept",
                "Origin",
                "Authorization",
                "Comment",
                "Country",
                "Station-Id",
                "NameOrEmail",
                "New-Password"
            )
            .allowedMethods("OPTIONS", "GET", "PUT", "POST", "DELETE", "HEAD")
            .allowedOriginPatterns("*")
    }

    @Bean
    fun resourceHttpMessageConverter(): ResourceHttpMessageConverter {
        val resourceHttpMessageConverter = ResourceHttpMessageConverter()
        resourceHttpMessageConverter.supportedMediaTypes = supportedMediaTypes
        return resourceHttpMessageConverter
    }

    @Bean
    fun methodValidationPostProcessor(): MethodValidationPostProcessor {
        return MethodValidationPostProcessor()
    }

    private val supportedMediaTypes: List<MediaType>
        get() = listOf(MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG, MediaType.APPLICATION_OCTET_STREAM)

    override fun addViewControllers(registry: ViewControllerRegistry) {
        registry.addViewController("/").setViewName("forward:/index.html")
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/")
        registry.addResourceHandler("/webjars/**").addResourceLocations("/webjars/").resourceChain(false)
    }

    @Bean
    fun validator(messageSource: MessageSource): LocalValidatorFactoryBean {
        val validatorFactoryBean = LocalValidatorFactoryBean()
        validatorFactoryBean.setValidationMessageSource(messageSource)
        return validatorFactoryBean
    }
}