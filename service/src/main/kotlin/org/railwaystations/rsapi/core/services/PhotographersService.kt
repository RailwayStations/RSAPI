package org.railwaystations.rsapi.core.services

import org.railwaystations.rsapi.adapter.db.CountryDao
import org.railwaystations.rsapi.adapter.db.StationDao
import org.railwaystations.rsapi.core.ports.LoadPhotographersUseCase
import org.springframework.stereotype.Service

@Service
class PhotographersService(
    private val stationDao: StationDao,
    private val countryDao: CountryDao,
) : LoadPhotographersUseCase {

    override fun getPhotographersPhotocountMap(country: String?): Map<String, Long> {
        require(!(country != null && countryDao.findById(country) == null)) { "Country $country does not exist" }
        return stationDao.getPhotographerMap(country)
    }
}
