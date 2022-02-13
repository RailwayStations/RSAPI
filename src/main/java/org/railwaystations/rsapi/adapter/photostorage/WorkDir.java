package org.railwaystations.rsapi.adapter.photostorage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class WorkDir {

    private final Path photosDir;
    private final Path inboxDir;
    private final Path inboxProcessedDir;
    private final Path inboxToProcessDir;
    private final Path inboxDoneDir;
    private final Path inboxRejectedDir;

    public WorkDir(@Value("${workDir}") final String workDir) {
        try {
            this.photosDir = Files.createDirectories(Path.of(workDir, "photos"));
            this.inboxDir = Path.of(workDir, "inbox");
            this.inboxProcessedDir = Files.createDirectories(inboxDir.resolve("processed"));
            this.inboxToProcessDir = Files.createDirectories(inboxDir.resolve("toprocess"));
            this.inboxDoneDir = Files.createDirectories(inboxDir.resolve("done"));
            this.inboxRejectedDir = Files.createDirectories(inboxDir.resolve("rejected"));
        } catch (final IOException e) {
            throw new RuntimeException("Unable to create working directories", e);
        }
    }

    public Path getPhotosDir() {
        return photosDir;
    }

    public Path getInboxDir() {
        return inboxDir;
    }

    public Path getInboxProcessedDir() {
        return inboxProcessedDir;
    }

    public Path getInboxToProcessDir() {
        return inboxToProcessDir;
    }

    public Path getInboxDoneDir() {
        return inboxDoneDir;
    }

    public Path getInboxRejectedDir() {
        return inboxRejectedDir;
    }
}
