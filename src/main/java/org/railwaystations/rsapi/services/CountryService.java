package org.railwaystations.rsapi.services;

import org.railwaystations.rsapi.adapter.db.CountryDao;
import org.railwaystations.rsapi.domain.model.Country;
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
