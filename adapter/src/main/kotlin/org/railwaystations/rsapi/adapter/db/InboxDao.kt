package org.railwaystations.rsapi.adapter.db

import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.sqlobject.config.RegisterRowMapper
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.customizer.BindList
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.railwaystations.rsapi.core.model.Coordinates
import org.railwaystations.rsapi.core.model.InboxEntry
import org.railwaystations.rsapi.core.model.ProblemReportType
import org.railwaystations.rsapi.core.model.PublicInboxEntry
import org.railwaystations.rsapi.core.ports.outbound.InboxPort
import java.sql.ResultSet
import java.sql.SQLException

private const val JOIN_QUERY: String = """
            SELECT i.id, i.countryCode, i.stationId, i.photoId, i.title i_title, s.title s_title, i.lat i_lat, i.lon i_lon, s.lat s_lat, s.lon s_lon,
                 i.photographerId, u.name photographerNickname, u.email photographerEmail, i.extension, i.comment, i.rejectReason, i.createdAt,
                 i.done, i.problemReportType, i.active, i.crc32, i.notified, p.urlPath, i.posted,
                 (
                    SELECT COUNT(*)
                        FROM inbox i2
                        WHERE i2.countryCode IS NOT NULL AND i2.countryCode = i.countryCode
                        AND i2.stationId IS NOT NULL AND i2.stationId = i.stationId AND i2.done = false AND i2.id != i.id
                 ) AS conflict
               FROM inbox i
                    LEFT JOIN stations s ON s.countryCode = i.countryCode AND s.id = i.stationId
                    LEFT JOIN users u ON u.id = i.photographerId
                    LEFT JOIN photos p ON p.countryCode = i.countryCode AND p.stationId = i.stationId AND ( ( p.primary = true AND i.photoId IS NULL) OR ( p.id = i.photoId ) )
            
            """
private const val COUNTRY_CODE: String = "countryCode"
private const val STATION_ID: String = "stationId"
private const val ID: String = "id"
private const val PHOTOGRAPHER_ID: String = "photographerId"
private const val COORDS: String = "coords"

interface InboxDao : InboxPort {
    @SqlQuery("$JOIN_QUERY WHERE i.id = :id")
    @RegisterRowMapper(
        InboxEntryMapper::class
    )
    override fun findById(@Bind(ID) id: Long): InboxEntry?

    @SqlQuery("$JOIN_QUERY WHERE i.done = false ORDER BY id")
    @RegisterRowMapper(
        InboxEntryMapper::class
    )
    override fun findPendingInboxEntries(): List<InboxEntry>

    @SqlQuery("$JOIN_QUERY WHERE i.done = true AND i.rejectReason IS NULL AND i.extension IS NOT NULL AND i.posted = false ORDER BY createdAt DESC LIMIT 1")
    @RegisterRowMapper(
        InboxEntryMapper::class
    )
    override fun findOldestImportedPhotoNotYetPosted(): InboxEntry?

    @SqlQuery(
        """
            SELECT i.countryCode, i.stationId, i.title i_title, s.title s_title, i.lat i_lat, i.lon i_lon, s.lat s_lat, s.lon s_lon
              FROM inbox i
                  LEFT JOIN stations s ON s.countryCode = i.countryCode AND s.id = i.stationId
              WHERE i.done = false AND (i.problemReportType IS NULL OR i.problemReportType = '')
            
            """
    )
    @RegisterRowMapper(PublicInboxEntryMapper::class)
    override fun findPublicInboxEntries(): List<PublicInboxEntry>

    @SqlUpdate(
        """
            INSERT INTO inbox (countryCode, stationId, photoId, title, lat, lon, photographerId, extension, comment, done, createdAt, problemReportType, active)
                    VALUES (:countryCode, :stationId, :photoId, :title, :lat, :lon, :photographerId, :extension, :comment, :done, :createdAt, :problemReportType, :active)
            
            """
    )
    @GetGeneratedKeys(ID)
    override fun insert(@BindBean inboxEntry: InboxEntry): Long

    @SqlUpdate("UPDATE inbox SET rejectReason = :rejectReason, done = true WHERE id = :id")
    override fun reject(@Bind(ID) id: Long, @Bind("rejectReason") rejectReason: String)

    @SqlUpdate("UPDATE inbox SET done = true WHERE id = :id")
    override fun done(@Bind(ID) id: Long)

    @SqlQuery("SELECT COUNT(*) FROM inbox WHERE countryCode = :countryCode AND stationId = :stationId AND done = false AND (:id IS NULL OR id <> :id)")
    override fun countPendingInboxEntriesForStation(
        @Bind(ID) id: Long?, @Bind(COUNTRY_CODE) countryCode: String, @Bind(STATION_ID) stationId: String
    ): Int

    @SqlQuery("SELECT COUNT(*) FROM inbox WHERE done = false")
    override fun countPendingInboxEntries(): Long

    /**
     * Count nearby pending uploads using simple pythagoras (only valid for a few km)
     */
    @SqlQuery("SELECT COUNT(*) FROM inbox WHERE SQRT(POWER(71.5 * (lon - :coords.lon),2) + POWER(111.3 * (lat - :coords.lat),2)) < 0.5 AND done = false AND (:id IS NULL OR id <> :id)")
    override fun countPendingInboxEntriesForNearbyCoordinates(
        @Bind(ID) id: Long?,
        @BindBean(COORDS) coordinates: Coordinates
    ): Int

    @SqlUpdate("UPDATE inbox SET crc32 = :crc32 WHERE id = :id")
    override fun updateCrc32(@Bind(ID) id: Long, @Bind("crc32") crc32: Long)

    @SqlQuery("$JOIN_QUERY WHERE i.done = true AND i.notified = false")
    @RegisterRowMapper(InboxEntryMapper::class)
    override fun findInboxEntriesToNotify(): List<InboxEntry>

    @SqlUpdate("UPDATE inbox SET notified = true WHERE id IN (<ids>)")
    override fun updateNotified(@BindList("ids") ids: List<Long>)

    @SqlUpdate("UPDATE inbox SET posted = true WHERE id = :id")
    override fun updatePosted(@Bind("id") id: Long)

    @SqlUpdate("UPDATE inbox SET photoId = :photoId WHERE id = :id")
    override fun updatePhotoId(@Bind("id") id: Long, @Bind("photoId") photoId: Long)

    @SqlUpdate("UPDATE inbox SET done = true, stationId = :stationId, countryCode = :countryCode, title = :title WHERE id = :id")
    override fun updateMissingStationImported(
        @Bind("id") id: Long,
        @Bind("countryCode") countryCode: String,
        @Bind("stationId") stationId: String,
        @Bind("title") title: String
    )

    @SqlQuery("$JOIN_QUERY WHERE i.photographerId = :photographerId AND (i.done = false OR :showCompletedEntries = true) ORDER BY i.id DESC")
    @RegisterRowMapper(InboxEntryMapper::class)
    override fun findByUser(
        @Bind(PHOTOGRAPHER_ID) photographerId: Int,
        @Bind("showCompletedEntries") showCompletedEntries: Boolean
    ): List<InboxEntry>

    @SqlQuery("$JOIN_QUERY WHERE i.countryCode = :countryCode AND i.stationId = :stationId AND i.done = false")
    @RegisterRowMapper(InboxEntryMapper::class)
    override fun findPendingByStation(
        @Bind("countryCode") countryCode: String,
        @Bind("stationId") stationId: String
    ): List<InboxEntry>

    class InboxEntryMapper : RowMapper<InboxEntry> {
        @Throws(SQLException::class)
        override fun map(rs: ResultSet, ctx: StatementContext): InboxEntry {
            val id = rs.getLong(ID)
            val done = rs.getBoolean("done")
            val problemReportType = rs.getString("problemReportType")
            val extension = rs.getString("extension")
            var active: Boolean? = rs.getBoolean("active")
            if (rs.wasNull()) {
                active = null
            }
            var crc32: Long? = rs.getLong("crc32")
            if (rs.wasNull()) {
                crc32 = null
            }
            var photoId: Long? = rs.getLong("photoId")
            if (rs.wasNull()) {
                photoId = null
            }
            return InboxEntry(
                id = id,
                countryCode = rs.getString(COUNTRY_CODE),
                stationId = rs.getString(STATION_ID),
                photoId = photoId,
                title = rs.getString("s_title"),
                newTitle = rs.getString("i_title"),
                coordinates = getCoordinates(rs, "s_"),
                newCoordinates = getCoordinates(rs, "i_"),
                photographerId = rs.getInt(PHOTOGRAPHER_ID),
                photographerNickname = rs.getString("photographerNickname"),
                photographerEmail = rs.getString("photographerEmail"),
                extension = extension,
                comment = rs.getString("comment"),
                rejectReason = rs.getString("rejectReason"),
                createdAt = rs.getTimestamp("createdAt").toInstant(),
                done = done,
                existingPhotoUrlPath = rs.getString("urlPath"),
                crc32 = crc32,
                conflict = rs.getInt("conflict") > 0,
                problemReportType = if (problemReportType != null) ProblemReportType.valueOf(problemReportType) else null,
                processed = false,
                inboxUrl = null,
                ds100 = null,
                active = active,
                createStation = null,
                notified = rs.getBoolean("notified"),
                posted = rs.getBoolean("posted")
            )
        }
    }

    class PublicInboxEntryMapper : RowMapper<PublicInboxEntry> {
        override fun map(rs: ResultSet, ctx: StatementContext): PublicInboxEntry {
            var title = rs.getString("s_title")
            var coordinates = getCoordinates(rs, "s_")
            val stationId = rs.getString(STATION_ID)
            if (stationId == null) {
                title = rs.getString("i_title")
                coordinates = getCoordinates(rs, "i_")
            }
            return PublicInboxEntry(
                countryCode = rs.getString(COUNTRY_CODE),
                stationId = stationId,
                title = title!!,
                coordinates = coordinates
            )
        }
    }

}

/**
 * Get the uploaded coordinates, if not present or not valid gets the station coordinates
 */
fun getCoordinates(rs: ResultSet, columnPrefix: String): Coordinates {
    return Coordinates(rs.getDouble(columnPrefix + "lat"), rs.getDouble(columnPrefix + "lon"))
}
