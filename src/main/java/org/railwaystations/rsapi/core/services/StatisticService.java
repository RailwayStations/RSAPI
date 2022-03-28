package org.railwaystations.rsapi.core.services;

import org.railwaystations.rsapi.adapter.out.db.CountryDao;
import org.railwaystations.rsapi.adapter.out.db.StationDao;
import org.railwaystations.rsapi.core.model.Statistic;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class StatisticService implements org.railwaystations.rsapi.core.ports.in.GetStatisticUseCase {

    private final CountryDao countryDao;
    private final StationDao stationDao;

    public StatisticService(final CountryDao countryDao, final StationDao stationDao) {
        super();
        this.countryDao = countryDao;
        this.stationDao = stationDao;
    }

    @Override
    public String getCountryStatisticMessage() {
        return "Countries statistic: \n" + countryDao.list(true).stream()
                .map(country -> getStatistic(country.getCode()))
                .map(statistic -> "- " + statistic.getCountryCode() + ": " + statistic.getWithPhoto() + " of " + statistic.getTotal())
                .collect(Collectors.joining("\n"));
    }

    @Override
    public Statistic getStatistic(final String country) {
        return stationDao.getStatistic(country);
    }

}
