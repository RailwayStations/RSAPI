package org.railwaystations.rsapi.core.ports.in;

import org.railwaystations.rsapi.core.model.InboxCommand;
import org.railwaystations.rsapi.core.model.InboxEntry;
import org.railwaystations.rsapi.core.model.InboxResponse;
import org.railwaystations.rsapi.core.model.InboxStateQuery;
import org.railwaystations.rsapi.core.model.ProblemReport;
import org.railwaystations.rsapi.core.model.PublicInboxEntry;
import org.railwaystations.rsapi.core.model.User;

import javax.validation.constraints.NotNull;
import java.io.InputStream;
import java.util.List;

public interface ManageInboxUseCase {

    InboxResponse reportProblem(ProblemReport problemReport, User user, String clientInfo);

    List<PublicInboxEntry> publicInbox();

    List<InboxStateQuery> userInbox(@NotNull User user, List<Long> ids);

    List<InboxEntry> listAdminInbox(@NotNull User user);

    long countPendingInboxEntries();

    String getNextZ();

    InboxResponse uploadPhoto(String clientInfo, InputStream body, String stationId,
                              String country, String contentType, String stationTitle,
                              Double latitude, Double longitude, String comment,
                              boolean active, User user);

    void changeStationTitle(InboxCommand command);

    void rejectInboxEntry(InboxCommand command);

    void importPhoto(InboxCommand command);

    void importMissingStation(InboxCommand command);

    void updateStationActiveState(InboxCommand command, boolean active);

    void deleteStation(InboxCommand command);

    void deletePrimaryPhoto(InboxCommand command);

    void markProblemReportSolved(InboxCommand command);

    void updateLocation(InboxCommand command);

    void markPrimaryPhotoOutdated(InboxCommand command);
}
