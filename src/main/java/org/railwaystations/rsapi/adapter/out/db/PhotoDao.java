package org.railwaystations.rsapi.adapter.out.db;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.railwaystations.rsapi.core.model.Photo;

public interface PhotoDao {

    @SqlUpdate("INSERT INTO photos (id, countryCode, stationId, primary, urlPath, license, photographerId, createdAt) VALUES (:id, :stationKey.country, :stationKey.id, :primary, :urlPath, :license, :photographer.id, :createdAt)")
    @GetGeneratedKeys("id")
    long insert(@BindBean Photo photo);

    @SqlUpdate("UPDATE photos SET primary = :primary, urlPath = :urlPath, license = :license, photographerId = :photographer.id, createdAt = :createdAt, outdated = :outdated WHERE id = :id")
    void update(@BindBean Photo photo);

    @SqlUpdate("DELETE FROM photos WHERE id = :id")
    void delete(@Bind("id") long id);

    @SqlUpdate("UPDATE photos SET outdated = true WHERE id = :id")
    void updatePhotoOutdated(@Bind("id")  long id);

}
