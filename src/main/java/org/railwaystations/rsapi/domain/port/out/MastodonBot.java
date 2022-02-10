package org.railwaystations.rsapi.domain.port.out;

import org.railwaystations.rsapi.domain.model.InboxEntry;
import org.railwaystations.rsapi.domain.model.Station;

public interface MastodonBot {

    void tootNewPhoto(final Station station, final InboxEntry inboxEntry);

}
