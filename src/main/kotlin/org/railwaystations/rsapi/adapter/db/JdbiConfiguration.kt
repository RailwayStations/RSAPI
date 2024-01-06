package org.railwaystations.rsapi.adapter.db

import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import javax.sql.DataSource

@Configuration
class JdbiConfiguration {
    @Bean
    fun jdbi(ds: DataSource): Jdbi {
        val proxy = TransactionAwareDataSourceProxy(ds)
        return Jdbi.create(proxy)
            .installPlugin(KotlinPlugin())
            .installPlugin(SqlObjectPlugin())
    }

    @Bean
    fun countryDao(jdbi: Jdbi): CountryDao {
        return jdbi.onDemand(CountryDao::class.java)
    }

    @Bean
    fun inboxDao(jdbi: Jdbi): InboxDao {
        return jdbi.onDemand(InboxDao::class.java)
    }

    @Bean
    fun photoDao(jdbi: Jdbi): PhotoDao {
        return jdbi.onDemand(PhotoDao::class.java)
    }

    @Bean
    fun stationDao(jdbi: Jdbi): StationDao {
        return jdbi.onDemand(StationDao::class.java)
    }

    @Bean
    fun userDao(jdbi: Jdbi): UserDao {
        return jdbi.onDemand(UserDao::class.java)
    }

    @Bean
    fun oAuth2AuthorizationDao(jdbi: Jdbi): OAuth2AuthorizationDao {
        return jdbi.onDemand(OAuth2AuthorizationDao::class.java)
    }
}
