package org.railwaystations.rsapi.core.services

import org.railwaystations.rsapi.adapter.db.CountryDao
import org.railwaystations.rsapi.core.model.Country
import org.railwaystations.rsapi.core.ports.ListCountriesUseCase
import org.springframework.stereotype.Service

@Service
class CountryService(private val countryDao: CountryDao) : ListCountriesUseCase {
    override fun list(onlyActive: Boolean?): Collection<Country> {
        return countryDao.list(onlyActive == null || onlyActive)
    }
}
