package org.railwaystations.rsapi.adapter.in.task;

import lombok.extern.slf4j.Slf4j;
import org.railwaystations.rsapi.adapter.out.db.InboxDao;
import org.railwaystations.rsapi.core.model.InboxEntry;
import org.railwaystations.rsapi.core.ports.out.PhotoStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
@ConditionalOnProperty(prefix = "webdavsync", name = "enabled", havingValue = "true")
@Slf4j
public class WebDavSyncTask {

    private final WebDavSyncConfig config;
    private final PhotoStorage photoStorage;
    private final InboxDao inboxDao;

    public WebDavSyncTask(final WebDavSyncConfig config, final PhotoStorage photoStorage, final InboxDao inboxDao) {
        super();
        this.config = config;
        this.photoStorage = photoStorage;
        this.inboxDao = inboxDao;
    }

    @Scheduled(fixedRate = 60_000)
    public void syncWebDav() {
        inboxDao.findPendingInboxEntries().stream()
                .filter(InboxEntry::isHasPhoto)
                .forEach(this::checkWebDav);
    }

    private void checkWebDav(final InboxEntry inboxEntry) {
        final var toProcessPath = photoStorage.getInboxToProcessFile(inboxEntry.getFilename());
        if (Files.exists(toProcessPath)) {
            try {
                uploadToProcess(toProcessPath);
                Files.delete(toProcessPath);
            } catch (final Exception e) {
                log.error("Unable to upload toProcess {}", toProcessPath, e);
            }
        }
    }

    private void uploadToProcess(final Path toProcessPath) {

    }

}
