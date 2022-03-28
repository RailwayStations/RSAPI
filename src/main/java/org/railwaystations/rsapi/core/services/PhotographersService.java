package org.railwaystations.rsapi.core.services;

import org.railwaystations.rsapi.adapter.out.db.StationDao;
import org.railwaystations.rsapi.core.ports.in.LoadPhotographersUseCase;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PhotographersService implements LoadPhotographersUseCase {

    private final StationDao stationDao;

    public PhotographersService(final StationDao stationDao) {
        super();
        this.stationDao = stationDao;
    }

    @Override
    public Map<String, Long> getPhotographersPhotocountMap(final String country) {
        return stationDao.getPhotographerMap(country);
    }

}
