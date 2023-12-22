package org.railwaystations.rsapi.adapter.in.web.controller;

import lombok.RequiredArgsConstructor;
import org.railwaystations.rsapi.adapter.in.web.api.StatisticApi;
import org.railwaystations.rsapi.adapter.in.web.model.StatisticDto;
import org.railwaystations.rsapi.core.model.Statistic;
import org.railwaystations.rsapi.core.ports.in.GetStatisticUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StatisticController implements StatisticApi {

    private final GetStatisticUseCase getStatisticUseCase;

    private StatisticDto getStatistic(String country) {
        return toDto(getStatisticUseCase.getStatistic(country));
    }

    private StatisticDto toDto(Statistic statistic) {
        return new StatisticDto(statistic.total(), statistic.withPhoto(), statistic.withoutPhoto(), statistic.photographers())
                .countryCode(statistic.countryCode());
    }

    @Override
    public ResponseEntity<StatisticDto> statsGet(String country) {
        return ResponseEntity.ok(getStatistic(country));
    }
}
