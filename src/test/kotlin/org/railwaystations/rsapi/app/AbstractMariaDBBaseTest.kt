package org.railwaystations.rsapi.app

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.utility.DockerImageName

abstract class AbstractMariaDBBaseTest {
    companion object {
        private val mariadb = MariaDBContainer(DockerImageName.parse("mariadb:10.8"))

        init {
            mariadb.withEnv("TZ", "UTC")
            mariadb.start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { mariadb.jdbcUrl }
            registry.add("spring.datasource.username") { mariadb.username }
            registry.add("spring.datasource.password") { mariadb.password }
        }
    }
}
