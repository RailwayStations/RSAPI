package org.railwaystations.rsapi.app;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class AbstractMariaDBBaseTest {

    private static final MariaDBContainer<?> mariadb;

    static {
        mariadb = new MariaDBContainer<>(DockerImageName.parse("mariadb:10.8"));
        mariadb.withEnv("TZ", "Etc/UTC");
        mariadb.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariadb::getJdbcUrl);
        registry.add("spring.datasource.username", mariadb::getUsername);
        registry.add("spring.datasource.password", mariadb::getPassword);
    }

}
