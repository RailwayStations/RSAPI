package org.railwaystations.rsapi.core.services;

import org.railwaystations.rsapi.adapter.out.db.CountryDao;
import org.railwaystations.rsapi.core.model.Country;
import org.railwaystations.rsapi.core.ports.in.ListCountriesUseCase;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class CountryService implements ListCountriesUseCase {

    private final CountryDao countryDao;

    public CountryService(CountryDao countryDao) {
        this.countryDao = countryDao;
    }

    @Override
    public Collection<Country> list(Boolean onlyActive) {
        return countryDao.list(onlyActive == null || onlyActive);
    }

}
