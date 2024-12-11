package org.railwaystations.rsapi.adapter.db

import org.jooq.DSLContext
import org.jooq.Record2
import org.jooq.Result
import org.jooq.SelectJoinStep
import org.jooq.impl.DSL.multiset
import org.jooq.impl.DSL.noCondition
import org.jooq.impl.DSL.selectFrom
import org.railwaystations.rsapi.adapter.db.jooq.tables.records.CountryRecord
import org.railwaystations.rsapi.adapter.db.jooq.tables.records.ProviderappRecord
import org.railwaystations.rsapi.adapter.db.jooq.tables.references.CountryTable
import org.railwaystations.rsapi.adapter.db.jooq.tables.references.ProviderappTable
import org.railwaystations.rsapi.core.model.Country
import org.railwaystations.rsapi.core.model.License
import org.railwaystations.rsapi.core.model.ProviderApp
import org.railwaystations.rsapi.core.ports.outbound.CountryPort
import org.springframework.stereotype.Component

@Component
class CountryAdapter(private val dsl: DSLContext) : CountryPort {

    override fun findById(id: String) =
        selectCountriesWithProviderApps()
            .where(CountryTable.id.eq(id))
            .fetchOne { record -> record.component1().toCountry(record.component2()) }

    private fun selectCountriesWithProviderApps(): SelectJoinStep<Record2<CountryRecord, Result<ProviderappRecord>>> =
        dsl.select(
            CountryTable,
            multiset(
                selectFrom(ProviderappTable)
                    .where(ProviderappTable.countrycode.eq(CountryTable.id))
            )
        ).from(CountryTable)

    private fun CountryRecord.toCountry(providerAppRecords: Result<ProviderappRecord>) = Country(
        code = id,
        name = name,
        _email = email,
        timetableUrlTemplate = timetableurltemplate,
        overrideLicense = overridelicense?.let { License.valueOf(it) },
        active = active == true,
        providerApps = providerAppRecords.map { it.toProviderApp() }
    )

    private fun ProviderappRecord.toProviderApp() = ProviderApp(
        type = type,
        name = name,
        url = url
    )

    override fun list(onlyActive: Boolean): Set<Country> {
        val condition = if (onlyActive) CountryTable.active.eq(true) else noCondition()
        return selectCountriesWithProviderApps()
            .where(condition)
            .fetch { record -> record.component1().toCountry(record.component2()) }
            .toSet()
    }

}
