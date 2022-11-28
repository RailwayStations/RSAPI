package org.railwaystations.rsapi.core.services;

import lombok.AllArgsConstructor;
import org.railwaystations.rsapi.adapter.out.db.CountryDao;
import org.railwaystations.rsapi.adapter.out.db.StationDao;
import org.railwaystations.rsapi.core.ports.in.LoadPhotographersUseCase;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
public class PhotographersService implements LoadPhotographersUseCase {

    private final StationDao stationDao;

    private final CountryDao countryDao;

    @Override
    public Map<String, Long> getPhotographersPhotocountMap(String country) {
        if (country != null && countryDao.findById(country).isEmpty()) {
            throw new IllegalArgumentException("Country " + country + " does not exist");
        }
        return stationDao.getPhotographerMap(country);
    }

}
