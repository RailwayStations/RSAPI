package org.railwaystations.rsapi.app.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.railwaystations.rsapi.adapter.in.web.writer.StationsGpxWriter;
import org.railwaystations.rsapi.adapter.in.web.writer.StatisticTxtWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@EnableWebMvc
@Configuration
public class WebConfig implements WebMvcConfigurer {


    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new StationsGpxWriter());
        converters.add(new StatisticTxtWriter());
        var builder = new Jackson2ObjectMapperBuilder();
        builder.serializationInclusion(JsonInclude.Include.NON_NULL);
        converters.add(new MappingJackson2HttpMessageConverter(builder.build()));
        converters.add(byteArrayHttpMessageConverter());
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry
                .addMapping("/**")
                .allowedHeaders("X-Requested-With","Content-Type","Accept","Origin","Authorization","Comment","Country","Station-Id","NameOrEmail","New-Password")
                .allowedMethods("OPTIONS","GET","PUT","POST","DELETE","HEAD")
                .allowedOriginPatterns("*");
    }

    @Bean
    public ByteArrayHttpMessageConverter byteArrayHttpMessageConverter() {
        var arrayHttpMessageConverter = new ByteArrayHttpMessageConverter();
        arrayHttpMessageConverter.setSupportedMediaTypes(getSupportedMediaTypes());
        return arrayHttpMessageConverter;
    }

    private List<MediaType> getSupportedMediaTypes() {
        return List.of(MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG, MediaType.APPLICATION_OCTET_STREAM);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
    }

}