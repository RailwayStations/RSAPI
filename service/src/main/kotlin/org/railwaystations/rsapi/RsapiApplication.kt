package org.railwaystations.rsapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.annotation.EnableTransactionManagement

@SpringBootApplication
@ComponentScan("org.railwaystations.rsapi")
@ConfigurationPropertiesScan("org.railwaystations.rsapi")
@EnableScheduling
@EnableAsync
@EnableTransactionManagement
class RsapiApplication

fun main(args: Array<String>) {
    runApplication<RsapiApplication>(*args)
}
