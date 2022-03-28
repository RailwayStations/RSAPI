package org.railwaystations.rsapi.core.ports.in;

import org.railwaystations.rsapi.core.model.Statistic;

public interface GetStatisticUseCase {
    String getCountryStatisticMessage();

    Statistic getStatistic(String country);
}
