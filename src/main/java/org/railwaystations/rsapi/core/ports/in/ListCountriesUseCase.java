package org.railwaystations.rsapi.core.ports.in;

import org.railwaystations.rsapi.core.model.Country;

import java.util.Collection;

public interface ListCountriesUseCase {
    Collection<Country> list(Boolean onlyActive);
}
