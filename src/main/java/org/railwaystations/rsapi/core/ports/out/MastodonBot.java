package org.railwaystations.rsapi.core.ports.out;

import org.springframework.scheduling.annotation.Async;

public interface MastodonBot {

    @Async
    void tootNewPhoto(String status);

}
