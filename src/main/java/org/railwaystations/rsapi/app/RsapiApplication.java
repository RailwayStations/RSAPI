package org.railwaystations.rsapi.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@ComponentScan("org.railwaystations.rsapi")
@ConfigurationPropertiesScan("org.railwaystations.rsapi")
@EnableScheduling
@EnableAsync
@EnableTransactionManagement
public class RsapiApplication {

	public static void main(final String[] args) {
		SpringApplication.run(RsapiApplication.class, args);
	}

}
