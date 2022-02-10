package org.railwaystations.rsapi.app;

import org.railwaystations.rsapi.domain.port.out.Monitor;
import org.railwaystations.rsapi.services.PhotoStationsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("PMD.BeanMembersShouldSerialize")
public class RsapiApplicationRunner implements CommandLineRunner {

    @Autowired
    private Monitor monitor;

    @Autowired
    private PhotoStationsService repository;

    @Override
    public void run(final String... args) {
        monitor.sendMessage(repository.getCountryStatisticMessage());
    }

}
