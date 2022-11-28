package org.railwaystations.rsapi.core.services;

import lombok.AllArgsConstructor;
import org.railwaystations.rsapi.adapter.out.db.CountryDao;
import org.railwaystations.rsapi.adapter.out.db.StationDao;
import org.railwaystations.rsapi.core.model.Country;
import org.railwaystations.rsapi.core.model.Statistic;
import org.springframework.stereotype.Service;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;

@Service
@AllArgsConstructor
public class StatisticService implements org.railwaystations.rsapi.core.ports.in.GetStatisticUseCase {

    private final CountryDao countryDao;
    private final StationDao stationDao;

    @Override
    public String getCountryStatisticMessage() {
        return "Countries statistic: \n" + countryDao.list(true).stream()
                .sorted(comparing(Country::getCode))
                .map(country -> getStatistic(country.getCode()))
                .map(statistic -> "- " + statistic.countryCode() + ": " + statistic.withPhoto() + " of " + statistic.total())
                .collect(joining("\n"));
    }

    @Override
    public Statistic getStatistic(String country) {
        if (country != null && countryDao.findById(country).isEmpty()) {
            throw new IllegalArgumentException("Country " + country + " does not exist");
        }
        return stationDao.getStatistic(country);
    }

}
