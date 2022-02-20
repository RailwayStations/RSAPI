package org.railwaystations.rsapi.adapter.monitoring;

import org.railwaystations.rsapi.core.ports.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
@Profile("!prod")
public class LoggingMonitor implements Monitor {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingMonitor.class);

    @Override
    public void sendMessage(final String message) {
        LOG.info(message);
    }

    @Override
    public void sendMessage(final String message, final Path file) {
        LOG.info(message + " - " + file);
    }

}
