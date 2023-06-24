package org.railwaystations.rsapi.adapter.in.task;

import lombok.extern.slf4j.Slf4j;
import org.railwaystations.rsapi.core.services.InboxService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MastodonBotTask {

    private final InboxService inboxService;

    public MastodonBotTask(InboxService inboxDao) {
        super();
        this.inboxService = inboxDao;
    }

    @Scheduled(fixedRate = 60_000 * 60) // every hour
    public void postNewPhoto() {
        log.info("Starting MastodonBotTask");
        inboxService.postRecentlyImportedPhotoNotYetPosted();
    }

}
