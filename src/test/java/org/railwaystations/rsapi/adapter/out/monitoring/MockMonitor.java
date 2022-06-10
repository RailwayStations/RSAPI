package org.railwaystations.rsapi.adapter.out.monitoring;

import org.railwaystations.rsapi.core.ports.out.Monitor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MockMonitor implements Monitor {

    private final List<String> messages = new ArrayList<>();

    @Override
    public void sendMessage(String message) {
        messages.add(message);
    }

    @Override
    public void sendMessage(String message, Path file) {
        messages.add(message);
    }

    public List<String> getMessages() {
        return messages;
    }

}
