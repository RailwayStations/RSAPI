package org.railwaystations.rsapi.adapter.in.task;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.railwaystations.rsapi.adapter.out.db.InboxDao;
import org.railwaystations.rsapi.core.model.InboxEntry;
import org.railwaystations.rsapi.core.ports.out.PhotoStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "webdavsync", name = "enabled", havingValue = "true")
@Slf4j
public class WebDavSyncTask {

    private final WebDavSyncConfig config;
    private final PhotoStorage photoStorage;
    private final InboxDao inboxDao;
    private final HttpClient client;

    public WebDavSyncTask(WebDavSyncConfig config, PhotoStorage photoStorage, InboxDao inboxDao) {
        super();
        this.config = config;
        this.photoStorage = photoStorage;
        this.inboxDao = inboxDao;

        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.of(10, ChronoUnit.SECONDS))
                .authenticator(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(config.user(), config.password().toCharArray());
                    }
                })
                .build();
    }

    @Scheduled(fixedRate = 60_000)
    public void syncWebDav() {
        log.info("Starting WebDavSync");
        var pendingInboxEntries = inboxDao.findPendingInboxEntries();
        if (pendingInboxEntries.isEmpty()) {
            return; // nothing to do
        }

        var processedFiles = listProcessedFiles();
        pendingInboxEntries.stream()
                .filter(InboxEntry::isPhotoUpload)
                .forEach(inboxEntry -> checkWebDav(inboxEntry, processedFiles));
    }

    private List<MultistatusResponse> listProcessedFiles() {
        log.info("ListProcessedFiles");
        var request = HttpRequest.newBuilder()
                .uri(URI.create(config.processedUrl()))
                .timeout(Duration.of(1, ChronoUnit.MINUTES))
                .method("PROPFIND", HttpRequest.BodyPublishers.ofString("""
                        <?xml version="1.0"?>
                        <a:propfind xmlns:a="DAV:">
                        <a:prop><a:resourcetype/></a:prop>
                        </a:propfind>"""))
                .header("Depth", "1")
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("ListProcessedFiles response " + response.statusCode());
            if (response.statusCode() != 207) {
                throw new RuntimeException("Failed to list processed files, statusCode=" + response.statusCode());
            }
            return new XmlMapper().readValue(response.body(), Multistatus.class).getResponses();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to list processed files", e);
        }
    }

    private void checkWebDav(InboxEntry inboxEntry, List<MultistatusResponse> processedFiles) {
        var toProcessPath = photoStorage.getInboxToProcessFile(inboxEntry.getFilename());
        if (Files.exists(toProcessPath)) {
            try {
                uploadToProcess(toProcessPath);
                Files.delete(toProcessPath);
            } catch (Exception e) {
                log.error("Unable to upload toProcess {}", toProcessPath, e);
            }
        }
        var processedPath = photoStorage.getInboxProcessedFile(inboxEntry.getFilename());
        if (checkIfDownloadProcessedNeeded(processedPath, processedFiles)) {
            try {
                downloadProcessed(processedPath);
            } catch (Exception e) {
                log.error("Unable to download of {}", processedPath, e);
            }
        }
    }

    private void downloadProcessed(Path processedPath) throws IOException, InterruptedException {
        log.info("Downloading processed file of {}", processedPath);
        var processedUri = URI.create(config.processedUrl() + "/" + processedPath.getFileName().toString());
        var getRequest = HttpRequest.newBuilder()
                .uri(processedUri)
                .timeout(Duration.of(1, ChronoUnit.MINUTES))
                .GET()
                .build();
        var getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofFile(processedPath));
        log.info("Download getResponse {}, bytes {}", getResponse.statusCode(), Files.size(processedPath));

        if (getResponse.statusCode() == 200) {
            var delRequest = HttpRequest.newBuilder()
                    .uri(processedUri)
                    .timeout(Duration.of(1, ChronoUnit.MINUTES))
                    .DELETE()
                    .build();
            var delResponse = client.send(delRequest, HttpResponse.BodyHandlers.ofFile(processedPath));
            log.info("Deleted {}, status {}", delRequest.uri(), delResponse.statusCode());
        } else {
            Files.deleteIfExists(processedPath);
        }
    }

    private boolean checkIfDownloadProcessedNeeded(Path processedPath, List<MultistatusResponse> processedFiles) {
        return processedFiles.stream()
                .anyMatch(multistatusResponse -> multistatusResponse.getHref().endsWith(processedPath.getFileName().toString()));
    }

    private void uploadToProcess(Path toProcessPath) throws IOException, InterruptedException {
        log.info("Uploading " + toProcessPath);
        var request = HttpRequest.newBuilder()
                .uri(URI.create(config.toProcessUrl() + "/" + toProcessPath.getFileName().toString()))
                .timeout(Duration.of(1, ChronoUnit.MINUTES))
                .PUT(HttpRequest.BodyPublishers.ofFile(toProcessPath))
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("Upload response " + response.statusCode());
        if (response.statusCode() != 201) {
            throw new RuntimeException("Failed Upload, statusCode=" + response.statusCode());
        }
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown=true)
    static class Multistatus {
        @JacksonXmlElementWrapper(useWrapping = false)
        @JsonProperty("response")
        List<MultistatusResponse> responses;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown=true)
    static class MultistatusResponse {
        String href;
    }

}
