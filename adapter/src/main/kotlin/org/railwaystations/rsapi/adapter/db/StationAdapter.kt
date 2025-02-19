package org.railwaystations.rsapi.adapter.db

import org.jooq.DSLContext
import org.jooq.Record12
import org.jooq.Record4
import org.jooq.SelectJoinStep
import org.jooq.SelectOnConditionStep
import org.jooq.impl.DSL.cast
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.countDistinct
import org.jooq.impl.DSL.max
import org.jooq.impl.DSL.or
import org.jooq.impl.DSL.power
import org.jooq.impl.DSL.sqrt
import org.jooq.impl.DSL.substring
import org.jooq.impl.DSL.value
import org.railwaystations.rsapi.adapter.db.jooq.tables.records.StationRecord
import org.railwaystations.rsapi.adapter.db.jooq.tables.references.PhotoTable
import org.railwaystations.rsapi.adapter.db.jooq.tables.references.StationTable
import org.railwaystations.rsapi.adapter.db.jooq.tables.references.UserTable
import org.railwaystations.rsapi.core.model.ANONYM
import org.railwaystations.rsapi.core.model.Coordinates
import org.railwaystations.rsapi.core.model.Photo
import org.railwaystations.rsapi.core.model.Station
import org.railwaystations.rsapi.core.model.Statistic
import org.railwaystations.rsapi.core.model.User
import org.railwaystations.rsapi.core.model.nameToLicense
import org.railwaystations.rsapi.core.ports.outbound.StationPort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Component
class StationAdapter(private val dsl: DSLContext) : StationPort {

    override fun findByCountryCodes(countryCodes: Set<String>, hasPhoto: Boolean?, active: Boolean?) =
        selectStationWithPrimaryPhoto()
            .where(StationTable.countrycode.`in`(countryCodes))
            .and(value(active).isNull.or(StationTable.active.eq(active)))
            .and(
                value(hasPhoto).isNull
                    .or(PhotoTable.urlpath.isNull.and(value(hasPhoto).isTrue))
                    .or(PhotoTable.urlpath.isNotNull.and(value(hasPhoto).isFalse))
            )
            .map { it.toPhotoStation() }
            .toSet()

    private fun selectStationWithPrimaryPhoto(): SelectOnConditionStep<Record12<StationRecord, Long?, Boolean?, String?, String?, Instant?, Boolean?, Long?, String?, String?, String?, Boolean?>> =
        selectPhotoStation()
            .leftJoin(PhotoTable).on(
                PhotoTable.countrycode.eq(StationTable.countrycode)
                    .and(PhotoTable.stationid.eq(StationTable.id).and(PhotoTable.primary.eq(true)))
            )
            .leftJoin(UserTable).on(UserTable.id.eq(PhotoTable.photographerid))

    private fun Record12<StationRecord, Long?, Boolean?, String?, String?, Instant?, Boolean?, Long?, String?, String?, String?, Boolean?>.toPhotoStation(): Station {
        val key = Station.Key(value1().countrycode, value1().id)
        val photo = value2()?.let {
            Photo(
                id = it,
                stationKey = key,
                primary = value3()!!,
                urlPath = value4()!!,
                photographer = User(
                    id = value8()!!,
                    name = value9()!!,
                    url = value10(),
                    license = value11()!!.nameToLicense(),
                    email = null,
                    ownPhotos = true,
                    anonymous = value12() == true,
                    key = null,
                    admin = false,
                    emailVerification = null,
                    newPassword = null,
                    sendNotifications = true,
                    locale = Locale.ENGLISH
                ),
                createdAt = value6()!!,
                license = value5()!!.nameToLicense(),
                outdated = value7() == true
            )
        }
        return Station(
            key = key,
            title = value1().title,
            coordinates = Coordinates(value1().lat, value1().lon),
            ds100 = value1().ds100,
            photos = photo?.let { listOf(it) } ?: emptyList(),
            active = value1().active
        )
    }

    override fun findByKey(key: Station.Key) =
        selectPhotoStation()
            .leftJoin(PhotoTable).on(
                PhotoTable.countrycode.eq(StationTable.countrycode)
                    .and(PhotoTable.stationid.eq(StationTable.id))
            )
            .leftJoin(UserTable).on(UserTable.id.eq(PhotoTable.photographerid))
            .where(StationTable.countrycode.eq(key.country).and(StationTable.id.eq(key.id)))
            .map { it.toPhotoStation() }
            .groupBy { it.key }
            .map { it.merge() }
            .toSet()
            .firstOrNull()

    private fun Map.Entry<Station.Key, List<Station>>.merge() = value.first().copy(photos = value.flatMap { it.photos })

    override fun findByPhotographer(photographer: String, countryCode: String?) =
        selectPhotoStation()
            .join(PhotoTable).on(
                PhotoTable.countrycode.eq(StationTable.countrycode)
                    .and(PhotoTable.stationid.eq(StationTable.id))
            )
            .join(UserTable).on(UserTable.id.eq(PhotoTable.photographerid))
            .where(value(countryCode).isNull.or(StationTable.countrycode.eq(countryCode)))
            .and(
                or(
                    UserTable.name.eq(photographer).and(UserTable.anonymous.eq(false)),
                    UserTable.anonymous.eq(true).and(value(photographer).eq(ANONYM))
                )
            )
            .map { it.toPhotoStation() }
            .groupBy { it.key }
            .map { it.merge() }
            .toSet()

    override fun findRecentImports(since: Instant) =
        selectPhotoStation()
            .join(PhotoTable).on(
                PhotoTable.countrycode.eq(StationTable.countrycode)
                    .and(PhotoTable.stationid.eq(StationTable.id))
            )
            .join(UserTable).on(UserTable.id.eq(PhotoTable.photographerid))
            .where(PhotoTable.createdat.gt(since))
            .map { it.toPhotoStation() }
            .toSet()

    private fun selectPhotoStation(): SelectJoinStep<Record12<StationRecord, Long?, Boolean?, String?, String?, Instant?, Boolean?, Long?, String?, String?, String?, Boolean?>> =
        dsl.select(
            StationTable,
            PhotoTable.id,
            PhotoTable.primary,
            PhotoTable.urlpath,
            PhotoTable.license,
            PhotoTable.createdat,
            PhotoTable.outdated,
            UserTable.id,
            UserTable.name,
            UserTable.url,
            UserTable.license,
            UserTable.anonymous
        )
            .from(StationTable)

    override fun getStatistic(countryCode: String?) =
        dsl.select(
            value(countryCode),
            count(StationTable.asterisk()),
            count(PhotoTable.urlpath),
            countDistinct(PhotoTable.photographerid)
        )
            .from(StationTable)
            .leftJoin(PhotoTable).on(
                PhotoTable.countrycode.eq(StationTable.countrycode)
                    .and(PhotoTable.stationid.eq(StationTable.id).and(PhotoTable.primary.eq(true)))
            )
            .where(StationTable.countrycode.eq(countryCode).or(value(countryCode).isNull))
            .fetchSingle().toStatistic()

    private fun Record4<String?, Int?, Int?, Int?>.toStatistic() = Statistic(
        countryCode = value1(),
        total = value2() ?: 0,
        withPhoto = value3() ?: 0,
        photographers = value4() ?: 0
    )

    override fun getPhotographerMap(countryCode: String?): Map<String, Int> =
        dsl.select(UserTable.name, count())
            .from(StationTable)
            .join(PhotoTable)
            .on(
                PhotoTable.countrycode.eq(StationTable.countrycode).and(PhotoTable.stationid.eq(StationTable.id))
                    .and(PhotoTable.primary.eq(true))
            )
            .join(UserTable).on(UserTable.id.eq(PhotoTable.photographerid))
            .where(value(countryCode).isNull.or(StationTable.countrycode.eq(countryCode)))
            .groupBy(UserTable.name)
            .fetch { it.value1()!! to it.value2() }.toMap()

    @Transactional
    override fun insert(station: Station) {
        dsl.executeInsert(
            StationRecord(
                id = station.key.id,
                countrycode = station.key.country,
                title = station.title,
                lat = station.coordinates.lat,
                lon = station.coordinates.lon,
                ds100 = station.ds100,
                active = station.active,
            )
        )
    }

    @Transactional
    override fun delete(key: Station.Key) {
        dsl.deleteFrom(StationTable)
            .where(StationTable.countrycode.eq(key.country).and(StationTable.id.eq(key.id)))
            .execute()
    }

    @Transactional
    override fun updateActive(key: Station.Key, active: Boolean) {
        dsl.update(StationTable)
            .set(StationTable.active, active)
            .where(StationTable.countrycode.eq(key.country).and(StationTable.id.eq(key.id)))
            .execute()
    }

    /**
     * Count nearby stations using simple pythagoras (only valid for a few km)
     */
    override fun countNearbyCoordinates(coordinates: Coordinates) =
        dsl.selectCount()
            .from(StationTable)
            .where(
                sqrt(
                    power(value(71.5).mul(StationTable.lon.minus(coordinates.lon)), 2)
                        .plus(power(value(111.3).mul(StationTable.lat.minus(coordinates.lat)), 2))
                ).lt(BigDecimal(0.5))
            )
            .fetchOne(0, Int::class.java) ?: 0

    override fun maxZ() =
        dsl.select(max(cast(substring(StationTable.id, 2), Integer::class.java)))
            .from(StationTable)
            .where(StationTable.id.like("Z%"))
            .fetchOne(0, Int::class.java) ?: 0

    @Transactional
    override fun changeStationTitle(key: Station.Key, newTitle: String) {
        dsl.update(StationTable)
            .set(StationTable.title, newTitle)
            .where(StationTable.countrycode.eq(key.country).and(StationTable.id.eq(key.id)))
            .execute()
    }

    @Transactional
    override fun updateLocation(key: Station.Key, coordinates: Coordinates) {
        dsl.update(StationTable)
            .set(StationTable.lat, coordinates.lat)
            .set(StationTable.lon, coordinates.lon)
            .where(StationTable.countrycode.eq(key.country).and(StationTable.id.eq(key.id)))
            .execute()
    }

    override fun findByPhotoId(photoId: Long) =
        selectStationWithPrimaryPhoto()
            .where(PhotoTable.id.eq(photoId))
            .fetchSingle().toPhotoStation()

}
