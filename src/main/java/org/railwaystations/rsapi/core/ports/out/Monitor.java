package org.railwaystations.rsapi.core.ports.out;

import java.nio.file.Path;

public interface Monitor {
    void sendMessage(String message);
    void sendMessage(String message, Path file);
}
