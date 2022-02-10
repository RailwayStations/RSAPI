package org.railwaystations.rsapi.domain.port.out;

import java.io.File;

public interface Monitor {
    void sendMessage(final String message);
    void sendMessage(final String message, File file);
}
