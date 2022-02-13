package org.railwaystations.rsapi.adapter.monitoring;

import org.railwaystations.rsapi.domain.port.out.Monitor;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MockMonitor implements Monitor {

    private final List<String> messages = new ArrayList<>();

    @Override
    public void sendMessage(final String message) {
        messages.add(message);
    }

    @Override
    public void sendMessage(final String message, final Path file) {
        messages.add(message);
    }

    public List<String> getMessages() {
        return messages;
    }

}
