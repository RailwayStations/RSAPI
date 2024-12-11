package org.railwaystations.rsapi.adapter.db

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.railwaystations.rsapi.core.model.CountryTestFixtures
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import

@JooqTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(
    JooqCustomizerConfiguration::class,
    CountryAdapter::class,
)
class CountryAdapterTest : AbstractPostgreSqlTest() {

    @Autowired
    private lateinit var sut: CountryAdapter

    @Test
    fun findById() {
        val de = sut.findById("de")

        assertThat(de).isEqualTo(CountryTestFixtures.countryDe)
    }

    @Test
    fun listOnlyActive() {
        val activeCountries = sut.list(true)

        assertThat(activeCountries).hasSize(2)
        assertThat(activeCountries.map { it.code }).containsExactlyInAnyOrder("de", "ch")
        assertThat(activeCountries.single { it.code == "de" }).isEqualTo(CountryTestFixtures.countryDe)
    }

    @Test
    fun listAll() {
        val activeCountries = sut.list(false)

        assertThat(activeCountries).hasSize(4)
        assertThat(activeCountries.map { it.code }).containsExactlyInAnyOrder("de", "ch", "it", "se")
        assertThat(activeCountries.single { it.code == "de" }).isEqualTo(CountryTestFixtures.countryDe)
    }

}
