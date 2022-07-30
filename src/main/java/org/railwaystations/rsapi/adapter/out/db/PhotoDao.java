package org.railwaystations.rsapi.adapter.out.db;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.railwaystations.rsapi.core.model.Photo;
import org.railwaystations.rsapi.core.model.Station;

public interface PhotoDao {

    @SqlUpdate("INSERT INTO photos (id, countryCode, stationId, urlPath, license, photographerId, createdAt) VALUES (:id, :stationKey.country, :stationKey.id, :urlPath, :license, :photographer.id, :createdAt)")
    @GetGeneratedKeys("id")
    Integer insert(@BindBean Photo photo);

    @SqlUpdate("UPDATE photos SET urlPath = :urlPath, license = :license, photographerId = :photographer.id, createdAt = :createdAt, outdated = :outdated WHERE countryCode = :stationKey.country AND stationId = :stationKey.id")
    void update(@BindBean Photo photo);

    @SqlUpdate("DELETE FROM photos WHERE countryCode = :country AND stationId = :id")
    void delete(@BindBean Station.Key key);

    @SqlUpdate("UPDATE photos SET outdated = true WHERE countryCode = :countryCode AND stationId = :stationId")
    void updatePhotoOutdated(@Bind("countryCode") String countryCode, @Bind("stationId") String stationId);

}
