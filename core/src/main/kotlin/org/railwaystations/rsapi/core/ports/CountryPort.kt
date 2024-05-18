package org.railwaystations.rsapi.core.ports

import org.railwaystations.rsapi.core.model.Country

interface CountryPort {
    fun findById(id: String): Country?
    fun list(onlyActive: Boolean): Set<Country>
}
