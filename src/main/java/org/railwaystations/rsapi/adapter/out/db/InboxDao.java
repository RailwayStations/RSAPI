package org.railwaystations.rsapi.adapter.out.db;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.railwaystations.rsapi.core.model.Coordinates;
import org.railwaystations.rsapi.core.model.InboxEntry;
import org.railwaystations.rsapi.core.model.ProblemReportType;
import org.railwaystations.rsapi.core.model.PublicInboxEntry;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public interface InboxDao {

    String JOIN_QUERY = """
                    SELECT u.id, u.countryCode, u.stationId, u.title u_title, s.title s_title, u.lat u_lat, u.lon u_lon, s.lat s_lat, s.lon s_lon,
                         u.photographerId, p.name photographerNickname, p.email photographerEmail, u.extension, u.comment, u.rejectReason, u.createdAt,
                         u.done, u.problemReportType, u.active, u.crc32, u.notified, f.urlPath,
                         (
                            SELECT COUNT(*)
                                FROM inbox u2
                                WHERE u2.countryCode IS NOT NULL AND u2.countryCode = u.countryCode
                                AND u2.stationId IS NOT NULL AND u2.stationId = u.stationId AND u2.done = false AND u2.id != u.id
                         ) AS conflict
                       FROM inbox u
                            LEFT JOIN stations s ON s.countryCode = u.countryCode AND s.id = u.stationId
                            LEFT JOIN users p ON p.id = u.photographerId
                            LEFT JOIN photos f ON f.countryCode = u.countryCode AND f.stationId = u.stationId AND f.primary = true
                    """;
    String COUNTRY_CODE = "countryCode";
    String STATION_ID = "stationId";
    String ID = "id";
    String PHOTOGRAPHER_ID = "photographerId";
    String COORDS = "coords";

    @SqlQuery(JOIN_QUERY + " WHERE u.id = :id")
    @RegisterRowMapper(InboxEntryMapper.class)
    InboxEntry findById(@Bind(ID) long id);

    @SqlQuery(JOIN_QUERY + " WHERE u.done = false ORDER BY id")
    @RegisterRowMapper(InboxEntryMapper.class)
    List<InboxEntry> findPendingInboxEntries();

    @SqlQuery("""
              SELECT u.countryCode, u.stationId, u.title u_title, s.title s_title, u.lat u_lat, u.lon u_lon, s.lat s_lat, s.lon s_lon
                FROM inbox u
                    LEFT JOIN stations s ON s.countryCode = u.countryCode AND s.id = u.stationId
                WHERE u.done = false AND (u.problemReportType IS NULL OR u.problemReportType = '')
              """)
    @RegisterRowMapper(PublicInboxEntryMapper.class)
    List<PublicInboxEntry> findPublicInboxEntries();

    @SqlUpdate("""
            INSERT INTO inbox (countryCode, stationId, title, lat, lon, photographerId, extension, comment, done, createdAt, problemReportType, active)
                    VALUES (:countryCode, :stationId, :title, :lat, :lon, :photographerId, :extension, :comment, :done, :createdAt, :problemReportType, :active)
            """)
    @GetGeneratedKeys(ID)
    Long insert(@BindBean InboxEntry inboxEntry);

    @SqlUpdate("UPDATE inbox SET rejectReason = :rejectReason, done = true WHERE id = :id")
    void reject(@Bind(ID) long id, @Bind("rejectReason") String rejectReason);

    @SqlUpdate("UPDATE inbox SET done = true WHERE id = :id")
    void done(@Bind(ID) long id);

    @SqlQuery("SELECT COUNT(*) FROM inbox WHERE countryCode = :countryCode AND stationId = :stationId AND done = false AND (:id IS NULL OR id <> :id)")
    int countPendingInboxEntriesForStation(@Bind(ID) Long id, @Bind(COUNTRY_CODE) String countryCode, @Bind(STATION_ID) String stationId);

    @SqlQuery("SELECT COUNT(*) FROM inbox WHERE done = false")
    long countPendingInboxEntries();

    /**
     * Count nearby pending uploads using simple pythagoras (only valid for a few km)
     */
    @SqlQuery("SELECT COUNT(*) FROM inbox WHERE SQRT(POWER(71.5 * (lon - :coords.lon),2) + POWER(111.3 * (lat - :coords.lat),2)) < 0.5 AND done = false AND (:id IS NULL OR id <> :id)")
    int countPendingInboxEntriesForNearbyCoordinates(@Bind(ID) Long id, @BindBean(COORDS) Coordinates coordinates);

    @SqlUpdate("UPDATE inbox SET crc32 = :crc32 WHERE id = :id")
    void updateCrc32(@Bind(ID) Long id, @Bind("crc32") Long crc32);

    @SqlQuery(JOIN_QUERY + " WHERE u.done = true AND u.notified = false")
    @RegisterRowMapper(InboxEntryMapper.class)
    List<InboxEntry> findInboxEntriesToNotify();

    @SqlUpdate("UPDATE inbox SET notified = true WHERE id IN (<ids>)")
    void updateNotified(@BindList("ids") List<Long> ids);

    class InboxEntryMapper implements RowMapper<InboxEntry> {

        public InboxEntry map(ResultSet rs, StatementContext ctx) throws SQLException {
            var id = rs.getLong(ID);
            var coordinates = getCoordinates(rs);
            var title = getTitle(rs);
            var done = rs.getBoolean("done");
            var problemReportType = rs.getString("problemReportType");
            var extension = rs.getString("extension");
            Boolean active = rs.getBoolean("active");
            if (rs.wasNull()) {
                active = null;
            }
            Long crc32 = rs.getLong("crc32");
            if (rs.wasNull()) {
                crc32 = null;
            }
            return InboxEntry.builder()
                    .id(id)
                    .countryCode(rs.getString(COUNTRY_CODE))
                    .stationId(rs.getString(STATION_ID))
                    .title(title)
                    .coordinates(coordinates)
                    .photographerId(rs.getInt(PHOTOGRAPHER_ID))
                    .photographerNickname(rs.getString("photographerNickname"))
                    .photographerEmail(rs.getString("photographerEmail"))
                    .extension(extension)
                    .comment(rs.getString("comment"))
                    .rejectReason(rs.getString("rejectReason"))
                    .createdAt(rs.getTimestamp("createdAt").toInstant())
                    .done(done)
                    .hasPhoto(rs.getString("urlPath") != null)
                    .conflict(rs.getInt("conflict") > 0)
                    .problemReportType(problemReportType != null ? ProblemReportType.valueOf(problemReportType) : null)
                    .active(active)
                    .crc32(crc32)
                    .notified(rs.getBoolean("notified"))
                    .build();
        }

    }

    class PublicInboxEntryMapper implements RowMapper<PublicInboxEntry> {

        public PublicInboxEntry map(ResultSet rs, StatementContext ctx) throws SQLException {
            return PublicInboxEntry.builder()
                    .countryCode(rs.getString(COUNTRY_CODE))
                    .stationId(rs.getString(STATION_ID))
                    .title(getTitle(rs))
                    .coordinates(getCoordinates(rs))
                    .build();
        }

    }

    /**
     * Gets the uploaded title, if not present returns the station title
     */
    static String getTitle(ResultSet rs) throws SQLException {
        var title = rs.getString("u_title");
        if (StringUtils.isBlank(title)) {
            title = rs.getString("s_title");
        }
        return title;
    }

    /**
     * Get the uploaded coordinates, if not present or not valid gets the station coordinates
     */
    static Coordinates getCoordinates(ResultSet rs) throws SQLException {
        var coordinates = new Coordinates(rs.getDouble("u_lat"), rs.getDouble("u_lon"));
        if (!coordinates.isValid()) {
            coordinates = new Coordinates(rs.getDouble("s_lat"), rs.getDouble("s_lon"));
        }
        return coordinates;
    }

}
