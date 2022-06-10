package org.railwaystations.rsapi.adapter.out.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.railwaystations.rsapi.core.ports.out.Monitor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
@ConditionalOnProperty(prefix = "monitor", name = "service", havingValue = "logging")
@Slf4j
public class LoggingMonitor implements Monitor {

    @Override
    public void sendMessage(String message) {
        log.info(message);
    }

    @Override
    public void sendMessage(String message, Path file) {
        log.info(message + " - " + file);
    }

}
