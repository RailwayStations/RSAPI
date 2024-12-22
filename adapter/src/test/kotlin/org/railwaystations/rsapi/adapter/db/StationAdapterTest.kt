package org.railwaystations.rsapi.adapter.db

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.railwaystations.rsapi.core.model.Coordinates
import org.railwaystations.rsapi.core.model.Station
import org.railwaystations.rsapi.core.model.StationTestFixtures.createStation
import org.railwaystations.rsapi.core.model.StationTestFixtures.keyCh8503001
import org.railwaystations.rsapi.core.model.StationTestFixtures.keyDe8000
import org.railwaystations.rsapi.core.model.StationTestFixtures.stationCh8503001
import org.railwaystations.rsapi.core.model.StationTestFixtures.stationDe8000
import org.railwaystations.rsapi.core.model.Statistic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import java.time.Instant

@JooqTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(
    JooqCustomizerConfiguration::class,
    StationAdapter::class,
)
class StationAdapterTest : AbstractPostgreSqlTest() {

    @Autowired
    private lateinit var sut: StationAdapter

    @ParameterizedTest
    @CsvSource(
        ",,730",
        "true,,646",
        "false,,84",
        ",true,729",
        ",false,1",
        "true,true,645",
        "false,false,0",
        "false,true,84",
        "true,false,1",
    )
    fun findByCountryCodesDe(hasPhoto: Boolean?, active: Boolean?, expectedCount: Int) {
        val stations = sut.findByCountryCodes(countryCodes = setOf("de"), hasPhoto = hasPhoto, active = active)

        assertThat(stations).hasSize(expectedCount)
    }

    @Test
    fun findByCountryCodesDeCh() {
        val stations = sut.findByCountryCodes(countryCodes = setOf("de", "ch"))

        assertThat(stations).hasSize(955)
        assertThat(stations.first { it.key == keyDe8000 }).isEqualTo(stationDe8000)
    }

    @Test
    fun findByKey() {
        val station = sut.findByKey(keyDe8000)

        assertThat(station).isEqualTo(stationDe8000)
    }

    @ParameterizedTest
    @CsvSource(
        "@user10,,16",
        "@user10,de,15",
        "@user10,ch,1",
        "Anonym,,10",
        "Anonym,de,9",
        "Anonym,ch,1",
    )
    fun findByPhotographer(photographer: String, countryCode: String? = null, expectedCount: Int) {
        val stations = sut.findByPhotographer(photographer = photographer, countryCode = countryCode)

        assertThat(stations).hasSize(expectedCount)
    }

    @Test
    fun findByPhotographerUser2() {
        val stations = sut.findByPhotographer(photographer = "@user2")

        assertThat(stations).hasSize(6)
        assertThat(stations.first { it.key == keyCh8503001 }).isEqualTo(stationCh8503001)
    }

    @ParameterizedTest
    @CsvSource(
        ",956,91,6",
        "ch,225,7,2",
        "de,730,84,4",
    )
    fun getStatistic(countryCode: String? = null, total: Int, withPhoto: Int, photographers: Int) {
        val statistic = sut.getStatistic(countryCode)

        assertThat(statistic).isEqualTo(
            Statistic(
                countryCode = countryCode,
                total = total,
                withPhoto = withPhoto,
                photographers = photographers,
            )
        )
    }

    @Test
    fun getPhotographerMapAll() {
        val photographers = sut.getPhotographerMap()

        assertThat(photographers).isEqualTo(
            mapOf<String, Int>(
                "@user27" to 31,
                "@user8" to 29,
                "@user10" to 15,
                "@user0" to 9,
                "@user2" to 6,
                "@user4" to 1,
            )
        )
    }

    @Test
    fun getPhotographerMapDe() {
        val photographers = sut.getPhotographerMap("de")

        assertThat(photographers).isEqualTo(
            mapOf<String, Int>(
                "@user27" to 31,
                "@user8" to 29,
                "@user10" to 15,
                "@user0" to 9,
            )
        )
    }

    @Test
    fun insert() {
        val newStation = createStation(key = Station.Key("de", "0815"), coordinates = Coordinates(12.3, 45.6))

        sut.insert(newStation)

        assertThat(sut.findByKey(newStation.key)).isEqualTo(newStation)
    }

    @Test
    fun delete() {
        assertThat(sut.findByKey(keyDe8000)).isNotNull

        sut.delete(keyDe8000)

        assertThat(sut.findByKey(keyDe8000)).isNull()
    }

    @Test
    fun updateActive() {
        assertThat(sut.findByKey(keyDe8000)!!.active).isTrue

        sut.updateActive(keyDe8000, false)

        assertThat(sut.findByKey(keyDe8000)!!.active).isFalse
    }

    @Test
    fun countNearbyCoordinates() {
        val count = sut.countNearbyCoordinates(Coordinates(48.45937, 7.95547))

        assertThat(count).isEqualTo(1)
    }

    @Test
    fun maxZ() {
        val maxZ = sut.maxZ()

        assertThat(maxZ).isEqualTo(1191)
    }

    @Test
    fun changeStationTitle() {
        val newTitle = "New Title"
        assertThat(sut.findByKey(keyDe8000)!!.title).isEqualTo(stationDe8000.title)

        sut.changeStationTitle(keyDe8000, newTitle)

        assertThat(sut.findByKey(keyDe8000)!!.title).isEqualTo(newTitle)
    }

    @Test
    fun updateLocation() {
        val newCoordinates = Coordinates(50.1, 9.2)
        assertThat(sut.findByKey(keyDe8000)!!.coordinates).isEqualTo(stationDe8000.coordinates)

        sut.updateLocation(keyDe8000, newCoordinates)

        assertThat(sut.findByKey(keyDe8000)!!.coordinates).isEqualTo(newCoordinates)
    }

    @Test
    fun findByPhotoId() {
        val station = sut.findByPhotoId(10)

        assertThat(station.key).isEqualTo(Station.Key("de", "6892"))
    }

    @Test
    fun findRecentImports() {
        val stations = sut.findRecentImports(Instant.parse("2022-08-01T00:00:00Z"))

        assertThat(stations.map { it.key }).containsExactlyInAnyOrder(
            Station.Key("de", "6932"),
            Station.Key("ch", "8500013"),
            Station.Key("ch", "8503087"),
        )
    }

}
