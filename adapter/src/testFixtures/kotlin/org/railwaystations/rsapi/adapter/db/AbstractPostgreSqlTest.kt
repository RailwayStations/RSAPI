package org.railwaystations.rsapi.adapter.db

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

abstract class AbstractPostgreSqlTest {
    companion object {
        private val postgresql =
            PostgreSQLContainer(DockerImageName.parse("postgres:17"))

        init {
            postgresql.start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgresql.jdbcUrl }
            registry.add("spring.datasource.username") { postgresql.username }
            registry.add("spring.datasource.password") { postgresql.password }
        }
    }
}