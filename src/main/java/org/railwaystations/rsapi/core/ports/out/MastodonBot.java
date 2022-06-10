package org.railwaystations.rsapi.core.ports.out;

import org.railwaystations.rsapi.core.model.InboxEntry;
import org.railwaystations.rsapi.core.model.Station;

public interface MastodonBot {

    void tootNewPhoto(Station station, InboxEntry inboxEntry);

}
