package org.railwaystations.rsapi.adapter.db;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.h2.H2DatabasePlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;

@Configuration
public class JdbiConfiguration {


    @Bean
    public Jdbi jdbi(final DataSource ds) {
        final TransactionAwareDataSourceProxy proxy = new TransactionAwareDataSourceProxy(ds);
        return Jdbi.create(proxy)
                .installPlugin(new H2DatabasePlugin())
                .installPlugin(new SqlObjectPlugin());
    }

    @Bean
    public CountryDao countryDao(final Jdbi jdbi) {
        return jdbi.onDemand(CountryDao.class);
    }

    @Bean
    public InboxDao inboxDao(final Jdbi jdbi) {
        return jdbi.onDemand(InboxDao.class);
    }

    @Bean
    public PhotoDao photoDao(final Jdbi jdbi) {
        return jdbi.onDemand(PhotoDao.class);
    }

    @Bean
    public StationDao stationDao(final Jdbi jdbi) {
        return jdbi.onDemand(StationDao.class);
    }

    @Bean
    public UserDao userDao(final Jdbi jdbi) {
        return jdbi.onDemand(UserDao.class);
    }

}