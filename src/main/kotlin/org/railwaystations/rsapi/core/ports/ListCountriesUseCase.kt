package org.railwaystations.rsapi.core.ports

import org.railwaystations.rsapi.core.model.Country

interface ListCountriesUseCase {
    fun list(onlyActive: Boolean?): Collection<Country>
}
