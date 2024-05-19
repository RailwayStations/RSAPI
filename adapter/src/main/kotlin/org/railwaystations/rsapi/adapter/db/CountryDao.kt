package org.railwaystations.rsapi.adapter.db

import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.result.LinkedHashMapRowReducer
import org.jdbi.v3.core.result.RowView
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.sqlobject.config.RegisterRowMapper
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.UseRowReducer
import org.railwaystations.rsapi.core.model.Country
import org.railwaystations.rsapi.core.model.License
import org.railwaystations.rsapi.core.model.ProviderApp
import org.railwaystations.rsapi.core.ports.outbound.CountryPort
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

interface CountryDao : CountryPort {
    @SqlQuery(
        """
            SELECT c.id c_id, c.name c_name, c.email c_email, c.timetableUrlTemplate c_timetableUrlTemplate,
                    c.overrideLicense c_overrideLicense, c.active c_active, p.type p_type, p.name p_name, p.url p_url
            FROM countries c
                LEFT JOIN providerApps p ON c.id = p.countryCode
            WHERE c.id = :id
            
            """
    )
    @UseRowReducer(CountryProviderAppReducer::class)
    @RegisterRowMapper(CountryMapper::class)
    @RegisterRowMapper(
        ProviderAppMapper::class
    )
    override fun findById(@Bind("id") id: String): Country?

    @SqlQuery(
        """
            SELECT c.id c_id, c.name c_name, c.email c_email, c.timetableUrlTemplate c_timetableUrlTemplate,
                    c.overrideLicense c_overrideLicense, c.active c_active, p.type p_type, p.name p_name, p.url p_url
            FROM countries c
                LEFT JOIN providerApps p ON c.id = p.countryCode
            WHERE :onlyActive = false OR c.active = true
            
            """
    )
    @UseRowReducer(CountryProviderAppReducer::class)
    @RegisterRowMapper(CountryMapper::class)
    @RegisterRowMapper(
        ProviderAppMapper::class
    )
    override fun list(@Bind("onlyActive") onlyActive: Boolean): Set<Country>

    class CountryMapper : RowMapper<Country> {
        @Throws(SQLException::class)
        override fun map(rs: ResultSet, ctx: StatementContext): Country {
            val overrideLicense = rs.getString("c_overrideLicense")
            return Country(
                code = rs.getString("c_id"),
                name = rs.getString("c_name"),
                email = rs.getString("c_email"),
                timetableUrlTemplate = rs.getString("c_timetableUrlTemplate"),
                overrideLicense = overrideLicense?.let { License.valueOf(it) },
                active = rs.getBoolean("c_active"),
            )
        }
    }

    class ProviderAppMapper : RowMapper<ProviderApp> {
        @Throws(SQLException::class)
        override fun map(rs: ResultSet, ctx: StatementContext): ProviderApp {
            return ProviderApp(
                type = rs.getString("p_type"),
                name = rs.getString("p_name"),
                url = rs.getString("p_url")
            )
        }
    }

    class CountryProviderAppReducer : LinkedHashMapRowReducer<String?, Country?> {
        override fun accumulate(container: MutableMap<String?, Country?>, rowView: RowView) {
            val country = container.computeIfAbsent(
                rowView.getColumn("c_id", String::class.java)
            ) { _: String? ->
                rowView.getRow(
                    Country::class.java
                )
            }

            if (rowView.getColumn("p_type", String::class.java) != null) {
                country?.providerApps?.add(rowView.getRow(ProviderApp::class.java))
            }
        }
    }
}
