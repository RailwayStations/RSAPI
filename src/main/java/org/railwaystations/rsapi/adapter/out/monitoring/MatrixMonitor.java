package org.railwaystations.rsapi.adapter.out.monitoring;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.railwaystations.rsapi.core.ports.out.Monitor;
import org.railwaystations.rsapi.utils.ImageUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Service
@ConditionalOnProperty(prefix = "monitor", name = "service", havingValue = "matrix")
@Slf4j
public class MatrixMonitor implements Monitor {

    private final ObjectMapper objectMapper;

    private final HttpClient client;

    private final MatrixMonitorConfig config;

    public MatrixMonitor(final MatrixMonitorConfig config, final ObjectMapper objectMapper) {
        super();
        this.config = config;
        this.objectMapper = objectMapper;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.of(10, ChronoUnit.SECONDS))
                .build();
    }

    @Override
    @Async
    public void sendMessage(final String message) {
        sendMessage(message, null);
    }

    @Override
    @Async
    public void sendMessage(final String message, final Path photo) {
        log.info("Sending message: {}", message);
        if (StringUtils.isBlank(config.roomUrl())) {
            log.warn("Skipping message, missing Matrix Room URL config");
            return;
        }
        try {
            final var response = sendRoomMessage(new MatrixTextMessage(message));
            final var status = response.statusCode();
            final var content = response.body();
            if (status >= 200 && status < 300) {
                log.info("Got json response: {}", content);
            } else {
                log.error("Error reading json, status {}: {}", status, content);
            }

            if (photo != null) {
                sendPhoto(photo);
            }
        } catch (final Exception e) {
            log.warn("Error sending MatrixMonitor message", e);
        }
    }

    private HttpResponse<String> sendRoomMessage(final Object message) throws Exception {
        final var json = objectMapper.writeValueAsString(message);

        final var request = HttpRequest.newBuilder()
                .uri(URI.create(config.roomUrl() + "?access_token=" + config.accessToken()))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
                .timeout(Duration.of(30, ChronoUnit.SECONDS))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void sendPhoto(final Path photo) throws Exception {
        final var request = HttpRequest.newBuilder()
                .uri(URI.create(config.uploadUrl() + "?filename=" + photo.getFileName() + "&access_token=" + config.accessToken()))
                .header("Content-Type", ImageUtil.extensionToMimeType(ImageUtil.getExtension(photo.getFileName().toString())))
                .timeout(Duration.of(1, ChronoUnit.MINUTES))
                .POST(HttpRequest.BodyPublishers.ofByteArray(ImageUtil.scalePhoto(photo, 300)))
                .build();

        final var responseUpload = client.send(request, HttpResponse.BodyHandlers.ofString());
        final var statusUpload = responseUpload.statusCode();
        final var contentUpload = responseUpload.body();
        if (statusUpload >= 200 && statusUpload < 300) {
            log.info("Got json response: {}", contentUpload);
        } else {
            log.error("Error reading json, statusUpload {}: {}", statusUpload, contentUpload);
            return;
        }

        final var matrixUploadResponse = objectMapper.readValue(contentUpload, MatrixUploadResponse.class);

        final var responseImage = sendRoomMessage(new MatrixImageMessage(photo.getFileName().toString(), matrixUploadResponse.contentUri));
        final var statusImage = responseImage.statusCode();
        final var contentImage = responseImage.body();
        if (statusImage >= 200 && statusImage < 300) {
            log.info("Got json response: {}", contentImage);
        } else {
            log.error("Error reading json, statusUpload {}: {}", statusImage, contentImage);
        }
    }

    private record MatrixTextMessage(String body) {
        public String getMsgtype() { return "m.text";}
    }

    private record MatrixImageMessage(String body, String url) {
        public String getMsgtype() { return "m.image";}
    }

    private record MatrixUploadResponse(@JsonProperty("content_uri") String contentUri) {}

}
