package org.railwaystations.rsapi.core.ports.out;

import org.railwaystations.rsapi.core.model.InboxEntry;
import org.railwaystations.rsapi.core.model.Photo;
import org.railwaystations.rsapi.core.model.Station;
import org.springframework.scheduling.annotation.Async;

public interface MastodonBot {

    @Async
    void tootNewPhoto(Station station, InboxEntry inboxEntry, Photo photo, long photoId);

}
