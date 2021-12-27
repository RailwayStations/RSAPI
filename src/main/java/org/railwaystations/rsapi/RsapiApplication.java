package org.railwaystations.rsapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@EnableTransactionManagement
public class RsapiApplication {

	public static void main(final String[] args) {
		SpringApplication.run(RsapiApplication.class, args);
	}

}