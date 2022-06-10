package org.railwaystations.rsapi.app;

import org.railwaystations.rsapi.core.ports.in.GetStatisticUseCase;
import org.railwaystations.rsapi.core.ports.out.Monitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RsapiApplicationRunner implements CommandLineRunner {

    @Autowired
    private Monitor monitor;

    @Autowired
    private GetStatisticUseCase getStatisticUseCase;

    @Override
    public void run(String... args) {
        monitor.sendMessage(getStatisticUseCase.getCountryStatisticMessage());
    }

}
