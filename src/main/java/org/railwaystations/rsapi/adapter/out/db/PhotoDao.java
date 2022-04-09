package org.railwaystations.rsapi.adapter.out.db;

import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.railwaystations.rsapi.core.model.Photo;
import org.railwaystations.rsapi.core.model.Station;

public interface PhotoDao {

    @SqlUpdate("INSERT INTO photos (countryCode, id, urlPath, license, photographerId, createdAt) VALUES (:stationKey.country, :stationKey.id, :urlPath, :license, :photographer.id, :createdAt)")
    void insert(@BindBean final Photo photo);

    @SqlUpdate("UPDATE photos SET urlPath = :urlPath, license = :license, photographerId = :photographer.id, createdAt = :createdAt WHERE countryCode = :stationKey.country AND id = :stationKey.id")
    void update(@BindBean final Photo photo);

    @SqlUpdate("DELETE FROM photos WHERE countryCode = :country AND id = :id")
    void delete(@BindBean final Station.Key key);

}
