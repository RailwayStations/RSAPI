package org.railwaystations.rsapi.adapter.db

import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.result.RowReducer
import org.jdbi.v3.core.result.RowView
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.sqlobject.SingleValue
import org.jdbi.v3.sqlobject.config.KeyColumn
import org.jdbi.v3.sqlobject.config.RegisterRowMapper
import org.jdbi.v3.sqlobject.config.ValueColumn
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.customizer.BindList
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.jdbi.v3.sqlobject.statement.UseRowReducer
import org.railwaystations.rsapi.core.model.Coordinates
import org.railwaystations.rsapi.core.model.License
import org.railwaystations.rsapi.core.model.License.Companion.of
import org.railwaystations.rsapi.core.model.Photo
import org.railwaystations.rsapi.core.model.Station
import org.railwaystations.rsapi.core.model.Statistic
import org.railwaystations.rsapi.core.model.User
import java.sql.ResultSet
import java.sql.SQLException
import java.time.Instant
import java.util.*
import java.util.stream.Stream

private const val JOIN_QUERY: String = """
            SELECT s.countryCode, s.id, s.DS100, s.title, s.lat, s.lon, s.active,
                    p.id AS photoId, p.primary, p.urlPath, p.license, p.createdAt, p.outdated, u.id AS photographerId,
                    u.name, u.url AS photographerUrl, u.license AS photographerLicense, u.anonymous
            FROM countries c
                LEFT JOIN stations s ON c.id = s.countryCode
                LEFT JOIN photos p ON p.countryCode = s.countryCode AND p.stationId = s.id AND p.primary = true
                LEFT JOIN users u ON u.id = p.photographerId
            
            """

interface StationDao {
    @SqlQuery("$JOIN_QUERY WHERE s.countryCode IN (<countryCodes>) AND (:active IS NULL OR s.active = :active) AND (:hasPhoto IS NULL OR (p.urlPath IS NULL AND :hasPhoto = false) OR (p.urlPath IS NOT NULL AND :hasPhoto = true))")
    @RegisterRowMapper(
        StationMapper::class
    )
    fun findByCountryCodes(
        @BindList("countryCodes") countryCodes: Set<String>,
        @Bind("hasPhoto") hasPhoto: Boolean?,
        @Bind("active") active: Boolean?
    ): Set<Station>

    @SqlQuery(
        """
            SELECT s.countryCode, s.id, s.DS100, s.title, s.lat, s.lon, s.active,
                    p.id AS photoId, p.primary, p.urlPath, p.license, p.createdAt, p.outdated, u.id AS photographerId,
                    u.name, u.url AS photographerUrl, u.license AS photographerLicense, u.anonymous
            FROM stations s
                LEFT JOIN photos p ON p.countryCode = s.countryCode AND p.stationId = s.id
                LEFT JOIN users u ON u.id = p.photographerId
            WHERE s.countryCode = :countryCode AND s.id = :id
            
            """
    )
    @UseRowReducer(SingleStationReducer::class)
    @RegisterRowMapper(SingleStationMapper::class)
    @RegisterRowMapper(
        PhotoMapper::class
    )
    fun findByKey(@Bind("countryCode") countryCode: String, @Bind("id") id: String): Station?

    @SqlQuery(
        """
            SELECT s.countryCode, s.id, s.DS100, s.title, s.lat, s.lon, s.active,
                    p.id AS photoId, p.primary, p.urlPath, p.license, p.createdAt, p.outdated, u.id AS photographerId,
                    u.name, u.url AS photographerUrl, u.license AS photographerLicense, u.anonymous
            FROM stations s
                LEFT JOIN photos p ON p.countryCode = s.countryCode AND p.stationId = s.id
                LEFT JOIN users u ON u.id = p.photographerId
            WHERE (:countryCode IS NULL OR s.countryCode = :countryCode) AND ((u.name = :photographer AND u.anonymous = false) OR (u.anonymous = true AND :photographer = 'Anonym'))
            
            """
    )
    @UseRowReducer(SingleStationReducer::class)
    @RegisterRowMapper(SingleStationMapper::class)
    @RegisterRowMapper(
        PhotoMapper::class
    )
    fun findByPhotographer(
        @Bind("photographer") photographer: String,
        @Bind("countryCode") countryCode: String?
    ): Set<Station>

    @SqlQuery(
        """
            SELECT s.countryCode, s.id, s.DS100, s.title, s.lat, s.lon, s.active,
                    p.id AS photoId, p.primary, p.urlPath, p.license, p.createdAt, p.outdated, u.id AS photographerId,
                    u.name, u.url AS photographerUrl, u.license AS photographerLicense, u.anonymous
            FROM stations s
                LEFT JOIN photos p ON p.countryCode = s.countryCode AND p.stationId = s.id
                LEFT JOIN users u ON u.id = p.photographerId
            WHERE createdAt > :since
            
            """
    )
    @UseRowReducer(SingleStationReducer::class)
    @RegisterRowMapper(SingleStationMapper::class)
    @RegisterRowMapper(
        PhotoMapper::class
    )
    fun findRecentImports(@Bind("since") since: Instant): Set<Station>

    class SingleStationReducer : RowReducer<MutableMap<Station.Key, Pair<Station, MutableList<Photo>>>, Station> {

        override fun accumulate(
            container: MutableMap<Station.Key, Pair<Station, MutableList<Photo>>>,
            rowView: RowView
        ) {
            val stationAndPhotos = container.computeIfAbsent(
                Station.Key(
                    country = rowView.getColumn("countryCode", String::class.java),
                    id = rowView.getColumn("id", String::class.java)
                )
            ) { _: Station.Key? ->
                rowView.getRow(Station::class.java) to mutableListOf()
            }

            if (rowView.getColumn("photoId", Integer::class.java) != null) {
                stationAndPhotos.second.add(rowView.getRow(Photo::class.java))
            }
        }

        override fun container(): MutableMap<Station.Key, Pair<Station, MutableList<Photo>>> {
            return mutableMapOf()
        }

        override fun stream(container: MutableMap<Station.Key, Pair<Station, MutableList<Photo>>>): Stream<Station> {
            return container.values.map {
                it.first.copy(
                    photos = it.second
                )
            }.stream()
        }


    }

    class SingleStationMapper : RowMapper<Station> {
        @Throws(SQLException::class)
        override fun map(rs: ResultSet, ctx: StatementContext): Station {
            val key = Station.Key(rs.getString("countryCode"), rs.getString("id"))
            return Station(
                key = key,
                title = rs.getString("title"),
                coordinates = Coordinates(rs.getDouble("lat"), rs.getDouble("lon")),
                ds100 = rs.getString("DS100"),
                photos = mutableListOf(),
                active = rs.getBoolean("active")
            )
        }
    }

    class PhotoMapper : RowMapper<Photo> {
        @Throws(SQLException::class)
        override fun map(rs: ResultSet, ctx: StatementContext): Photo {
            val key = Station.Key(rs.getString("countryCode"), rs.getString("id"))
            val photoUrlPath = rs.getString("urlPath")
            return Photo(
                id = rs.getLong("photoId"),
                stationKey = key,
                primary = rs.getBoolean("primary"),
                urlPath = photoUrlPath,
                photographer = User(
                    id = rs.getInt("photographerId"),
                    name = rs.getString("name"),
                    url = rs.getString("photographerUrl"),
                    license = of(rs.getString("photographerLicense")),
                    email = null,
                    ownPhotos = true,
                    anonymous = rs.getBoolean("anonymous"),
                    key = null,
                    admin = false,
                    emailVerification = null,
                    newPassword = null,
                    sendNotifications = true,
                    locale = Locale.ENGLISH
                ),
                createdAt = rs.getTimestamp("createdAt").toInstant(),
                license = License.valueOf(rs.getString("license")),
                outdated = rs.getBoolean("outdated")
            )
        }
    }


    @SqlQuery(
        """
            SELECT :countryCode countryCode, COUNT(*) stations, COUNT(p.urlPath) photos, COUNT(distinct p.photographerId) photographers
            FROM stations s
                LEFT JOIN photos p ON p.countryCode = s.countryCode AND p.stationId = s.id AND p.primary = true
            WHERE s.countryCode = :countryCode OR :countryCode IS NULL
            """
    )
    @RegisterRowMapper(StatisticMapper::class)
    @SingleValue
    fun getStatistic(@Bind("countryCode") countryCode: String?): Statistic

    @SqlQuery(
        """
            SELECT u.name photographer, COUNT(*) photocount
            FROM stations s
                JOIN photos p ON p.countryCode = s.countryCode AND p.stationId = s.id AND p.primary = true
                JOIN users u ON u.id = p.photographerId
            WHERE s.countryCode = :countryCode OR :countryCode IS NULL
            GROUP BY u.name
            ORDER BY COUNT(*) DESC
            """
    )
    @KeyColumn("photographer")
    @ValueColumn("photocount")
    fun getPhotographerMap(@Bind("countryCode") countryCode: String?): Map<String, Long>

    @SqlUpdate("INSERT INTO stations (countryCode, id, title, lat, lon, ds100, active) VALUES (:key.country, :key.id, :title, :coordinates?.lat, :coordinates?.lon, :ds100, :active)")
    fun insert(@BindBean station: Station)

    @SqlUpdate("DELETE FROM stations WHERE countryCode = :country AND id = :id")
    fun delete(@BindBean key: Station.Key)

    @SqlUpdate("UPDATE stations SET active = :active WHERE countryCode = :key.country AND id = :key.id")
    fun updateActive(@BindBean("key") key: Station.Key, @Bind("active") active: Boolean)

    /**
     * Count nearby stations using simple pythagoras (only valid for a few km)
     */
    @SqlQuery("SELECT COUNT(*) FROM stations WHERE SQRT(POWER(71.5 * (lon - :lon),2) + POWER(111.3 * (lat - :lat),2)) < 0.5")
    fun countNearbyCoordinates(@BindBean coordinates: Coordinates): Int

    @get:SqlQuery("SELECT MAX(CAST(substring(id,2) AS INT)) FROM stations WHERE id LIKE 'Z%'")
    val maxZ: Int

    @SqlUpdate("UPDATE stations SET title = :new_title WHERE countryCode = :key.country AND id = :key.id")
    fun changeStationTitle(@BindBean("key") key: Station.Key, @Bind("new_title") newTitle: String)

    @SqlUpdate("UPDATE stations SET lat = :coords.lat, lon = :coords.lon WHERE countryCode = :key.country AND id = :key.id")
    fun updateLocation(@BindBean("key") key: Station.Key, @BindBean("coords") coordinates: Coordinates)

    class StationMapper : RowMapper<Station> {
        @Throws(SQLException::class)
        override fun map(rs: ResultSet, ctx: StatementContext): Station {
            val key = Station.Key(rs.getString("countryCode"), rs.getString("id"))
            val photos = buildList {
                val photoUrlPath = rs.getString("urlPath")
                if (photoUrlPath != null) {
                    add(
                        Photo(
                            id = rs.getLong("photoId"),
                            stationKey = key,
                            primary = rs.getBoolean("primary"),
                            urlPath = photoUrlPath,
                            photographer = User(
                                id = rs.getInt("photographerId"),
                                name = rs.getString("name"),
                                url = rs.getString("photographerUrl"),
                                license = of(rs.getString("photographerLicense")),
                                ownPhotos = true,
                                anonymous = rs.getBoolean("anonymous"),
                            ),
                            createdAt = rs.getTimestamp("createdAt").toInstant(),
                            license = License.valueOf(rs.getString("license")),
                            outdated = rs.getBoolean("outdated")
                        )
                    )
                }
            }

            return Station(
                key = key,
                title = rs.getString("title"),
                coordinates = Coordinates(rs.getDouble("lat"), rs.getDouble("lon")),
                ds100 = rs.getString("DS100"),
                photos = photos,
                active = rs.getBoolean("active")
            )
        }
    }

    class StatisticMapper : RowMapper<Statistic> {
        @Throws(SQLException::class)
        override fun map(rs: ResultSet, ctx: StatementContext): Statistic {
            return Statistic(
                countryCode = rs.getString("countryCode"),
                total = rs.getLong("stations"),
                withPhoto = rs.getLong("photos"),
                photographers = rs.getLong("photographers")
            )
        }
    }

}
