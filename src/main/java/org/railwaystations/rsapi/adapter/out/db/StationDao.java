package org.railwaystations.rsapi.adapter.out.db;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SingleValue;
import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.config.ValueColumn;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;
import org.railwaystations.rsapi.core.model.Coordinates;
import org.railwaystations.rsapi.core.model.License;
import org.railwaystations.rsapi.core.model.Photo;
import org.railwaystations.rsapi.core.model.Station;
import org.railwaystations.rsapi.core.model.Statistic;
import org.railwaystations.rsapi.core.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

public interface StationDao {

    String JOIN_QUERY = """
            SELECT s.countryCode, s.id, s.DS100, s.title, s.lat, s.lon, s.active,
                    p.id AS photoId, p.primary, p.urlPath, p.license, p.createdAt, p.outdated, u.id AS photographerId,
                    u.name, u.url AS photographerUrl, u.license AS photographerLicense, u.anonymous
            FROM countries c
                LEFT JOIN stations s ON c.id = s.countryCode
                LEFT JOIN photos p ON p.countryCode = s.countryCode AND p.stationId = s.id AND p.primary = true
                LEFT JOIN users u ON u.id = p.photographerId
            """;

    @SqlQuery(JOIN_QUERY + " WHERE s.countryCode IN (<countryCodes>) AND ((c.active = true AND :active IS NULL) OR c.active = :active) AND (:hasPhoto IS NULL OR (p.urlPath IS NULL AND :hasPhoto = false) OR (p.urlPath IS NOT NULL AND :hasPhoto = true))")
    @RegisterRowMapper(StationMapper.class)
    Set<Station> findByCountryCodes(@BindList("countryCodes") Set<String> countryCodes, @Bind("hasPhoto") Boolean hasPhoto, @Bind("active") Boolean active);

    @SqlQuery(JOIN_QUERY + " WHERE ((c.active = true AND :active IS NULL) OR c.active = :active) AND (:hasPhoto IS NULL OR (p.urlPath IS NULL AND :hasPhoto = false) OR (p.urlPath IS NOT NULL AND :hasPhoto = true))")
    @RegisterRowMapper(StationMapper.class)
    Set<Station> findByAllCountries(@Bind("hasPhoto") Boolean hasPhoto, @Bind("active") Boolean active);

    @SqlQuery("""
            SELECT s.countryCode, s.id, s.DS100, s.title, s.lat, s.lon, s.active,
                    p.id AS photoId, p.primary, p.urlPath, p.license, p.createdAt, p.outdated, u.id AS photographerId,
                    u.name, u.url AS photographerUrl, u.license AS photographerLicense, u.anonymous
            FROM stations s
                LEFT JOIN photos p ON p.countryCode = s.countryCode AND p.stationId = s.id
                LEFT JOIN users u ON u.id = p.photographerId
            WHERE s.countryCode = :countryCode AND s.id = :id
            """)
    @UseRowReducer(SingleStationReducer.class)
    @RegisterRowMapper(SingleStationMapper.class)
    @RegisterRowMapper(PhotoMapper.class)
    Set<Station> findByKey(@Bind("countryCode") String countryCode, @Bind("id") String id);

    @SqlQuery("""
            SELECT s.countryCode, s.id, s.DS100, s.title, s.lat, s.lon, s.active,
                    p.id AS photoId, p.primary, p.urlPath, p.license, p.createdAt, p.outdated, u.id AS photographerId,
                    u.name, u.url AS photographerUrl, u.license AS photographerLicense, u.anonymous
            FROM stations s
                LEFT JOIN photos p ON p.countryCode = s.countryCode AND p.stationId = s.id
                LEFT JOIN users u ON u.id = p.photographerId
            WHERE (:countryCode IS NULL OR s.countryCode = :countryCode) AND ((u.name = :photographer AND u.anonymous = false) OR (u.anonymous = true AND :photographer = 'Anonym'))
            """)
    @UseRowReducer(SingleStationReducer.class)
    @RegisterRowMapper(SingleStationMapper.class)
    @RegisterRowMapper(PhotoMapper.class)
    Set<Station> findByPhotographer(@Bind("photographer") String photographer, @Bind("countryCode") String countryCode);

    class SingleStationReducer implements LinkedHashMapRowReducer<Station.Key, Station> {
        @Override
        public void accumulate(Map<Station.Key, Station> map, RowView rowView) {
            var station = map.computeIfAbsent(
                    new Station.Key(rowView.getColumn("countryCode", String.class),
                            rowView.getColumn("id", String.class)),
                    id -> rowView.getRow(Station.class));

            if (rowView.getColumn("photoId", Long.class) != null) {
                station.getPhotos().add(rowView.getRow(Photo.class));
            }
        }
    }

    class SingleStationMapper implements RowMapper<Station> {

        public Station map(ResultSet rs, StatementContext ctx) throws SQLException {
            var key = new Station.Key(rs.getString("countryCode"), rs.getString("id"));
            return Station.builder()
                    .key(key)
                    .title(rs.getString("title"))
                    .coordinates(new Coordinates(rs.getDouble("lat"), rs.getDouble("lon")))
                    .ds100(rs.getString("DS100"))
                    .active(rs.getBoolean("active"))
                    .build();
        }

    }

    class PhotoMapper implements RowMapper<Photo> {

        public Photo map(ResultSet rs, StatementContext ctx) throws SQLException {
            var key = new Station.Key(rs.getString("countryCode"), rs.getString("id"));
            var photoUrlPath = rs.getString("urlPath");
            return Photo.builder()
                    .id(rs.getLong("photoId"))
                    .stationKey(key)
                    .primary(rs.getBoolean("primary"))
                    .urlPath(photoUrlPath)
                    .photographer(User.builder()
                            .name(rs.getString("name"))
                            .url(rs.getString("photographerUrl"))
                            .license(License.valueOf(rs.getString("photographerLicense")))
                            .id(rs.getInt("photographerId"))
                            .anonymous(rs.getBoolean("anonymous"))
                            .build())
                    .createdAt(rs.getTimestamp("createdAt").toInstant())
                    .license(License.valueOf(rs.getString("license")))
                    .outdated(rs.getBoolean("outdated"))
                    .build();
        }

    }


    @SqlQuery("""
            SELECT :countryCode countryCode, COUNT(*) stations, COUNT(p.urlPath) photos, COUNT(distinct p.photographerId) photographers
            FROM stations s
                LEFT JOIN photos p ON p.countryCode = s.countryCode AND p.stationId = s.id AND p.primary = true
            WHERE s.countryCode = :countryCode OR :countryCode IS NULL
            """)
    @RegisterRowMapper(StatisticMapper.class)
    @SingleValue
    Statistic getStatistic(@Bind("countryCode") String countryCode);

    @SqlQuery("""
            SELECT u.name photographer, COUNT(*) photocount
            FROM stations s
                JOIN photos p ON p.countryCode = s.countryCode AND p.stationId = s.id AND p.primary = true
                JOIN users u ON u.id = p.photographerId
            WHERE s.countryCode = :countryCode OR :countryCode IS NULL
            GROUP BY u.name
            ORDER BY COUNT(*) DESC
            """)
    @KeyColumn("photographer")
    @ValueColumn("photocount")
    Map<String, Long> getPhotographerMap(@Bind("countryCode") String countryCode);

    @SqlUpdate("INSERT INTO stations (countryCode, id, title, lat, lon, ds100, active) VALUES (:key.country, :key.id, :title, :coordinates?.lat, :coordinates?.lon, :ds100, :active)")
    void insert(@BindBean Station station);

    @SqlUpdate("DELETE FROM stations WHERE countryCode = :country AND id = :id")
    void delete(@BindBean Station.Key key);

    @SqlUpdate("UPDATE stations SET active = :active WHERE countryCode = :key.country AND id = :key.id")
    void updateActive(@BindBean("key") Station.Key key, @Bind("active") boolean active);

    @SqlQuery(JOIN_QUERY + " WHERE createdAt > :since ORDER BY createdAt DESC")
    @RegisterRowMapper(StationMapper.class)
    Set<Station> findRecentImports(@Bind("since") Instant since);

    /**
     * Count nearby stations using simple pythagoras (only valid for a few km)
     */
    @SqlQuery("SELECT COUNT(*) FROM stations WHERE SQRT(POWER(71.5 * (lon - :lon),2) + POWER(111.3 * (lat - :lat),2)) < 0.5")
    int countNearbyCoordinates(@BindBean Coordinates coordinates);

    @SqlQuery("SELECT MAX(CAST(substring(id,2) AS INT)) FROM stations WHERE id LIKE 'Z%'")
    int getMaxZ();

    @SqlUpdate("UPDATE stations SET title = :new_title WHERE countryCode = :key.country AND id = :key.id")
    void changeStationTitle(@BindBean("key") Station.Key key, @Bind("new_title") String newTitle);

    @SqlUpdate("UPDATE stations SET lat = :coords.lat, lon = :coords.lon WHERE countryCode = :key.country AND id = :key.id")
    void updateLocation(@BindBean("key") Station.Key key, @BindBean("coords") Coordinates coordinates);

    class StationMapper implements RowMapper<Station> {

        public Station map(ResultSet rs, StatementContext ctx) throws SQLException {
            var key = new Station.Key(rs.getString("countryCode"), rs.getString("id"));
            var station = Station.builder()
                    .key(key)
                    .title(rs.getString("title"))
                    .coordinates(new Coordinates(rs.getDouble("lat"), rs.getDouble("lon")))
                    .ds100(rs.getString("DS100"))
                    .active(rs.getBoolean("active"))
                    .build();

            var photoUrlPath = rs.getString("urlPath");
            if (photoUrlPath != null) {
                station.getPhotos().add(Photo.builder()
                        .id(rs.getLong("photoId"))
                        .stationKey(key)
                        .primary(rs.getBoolean("primary"))
                        .urlPath(photoUrlPath)
                        .photographer(User.builder()
                                .name(rs.getString("name"))
                                .url(rs.getString("photographerUrl"))
                                .license(License.valueOf(rs.getString("photographerLicense")))
                                .id(rs.getInt("photographerId"))
                                .anonymous(rs.getBoolean("anonymous"))
                                .build())
                        .createdAt(rs.getTimestamp("createdAt").toInstant())
                        .license(License.valueOf(rs.getString("license")))
                        .outdated(rs.getBoolean("outdated"))
                        .build());
            }

            return station;
        }

    }

    class StatisticMapper implements RowMapper<Statistic> {
        @Override
        public Statistic map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new Statistic(rs.getString("countryCode"), rs.getLong("stations"), rs.getLong("photos"), rs.getLong("photographers"));
        }
    }

}
