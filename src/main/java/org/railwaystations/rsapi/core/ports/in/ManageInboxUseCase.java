package org.railwaystations.rsapi.core.ports.in;

import org.railwaystations.rsapi.core.model.*;

import javax.validation.constraints.NotNull;
import java.io.InputStream;
import java.util.List;

public interface ManageInboxUseCase {

    InboxResponse reportProblem(ProblemReport problemReport, User user, String clientInfo);

    List<PublicInboxEntry> publicInbox();

    List<InboxStateQuery> userInbox(@NotNull User user, @NotNull List<InboxStateQuery> queries);

    List<InboxEntry> listAdminInbox(@NotNull User user);

    void processAdminInboxCommand(@NotNull User user, @NotNull InboxEntry command);

    int countPendingInboxEntries();

    String getNextZ();

    InboxResponse uploadPhoto(String clientInfo, InputStream body, String stationId,
                              String country, String contentType, String stationTitle,
                              Double latitude, Double longitude, String comment,
                              Boolean active, User user);
}
