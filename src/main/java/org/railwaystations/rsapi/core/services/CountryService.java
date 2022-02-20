package org.railwaystations.rsapi.core.services;

import org.railwaystations.rsapi.adapter.db.CountryDao;
import org.railwaystations.rsapi.core.model.Country;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class CountryService {

    private final CountryDao countryDao;

    public CountryService(final CountryDao countryDao) {
        this.countryDao = countryDao;
    }

    public Collection<Country> list(final Boolean onlyActive) {
        return countryDao.list(onlyActive == null || onlyActive);
    }

}
