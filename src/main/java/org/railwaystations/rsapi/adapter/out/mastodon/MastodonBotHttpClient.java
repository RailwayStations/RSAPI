package org.railwaystations.rsapi.adapter.out.mastodon;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.railwaystations.rsapi.core.model.InboxEntry;
import org.railwaystations.rsapi.core.model.Photo;
import org.railwaystations.rsapi.core.model.Station;
import org.railwaystations.rsapi.core.ports.out.MastodonBot;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
public class MastodonBotHttpClient implements MastodonBot {

    private final ObjectMapper objectMapper;

    private final MastodonBotConfig config;

    private final HttpClient client;

    public MastodonBotHttpClient(MastodonBotConfig config, ObjectMapper objectMapper) {
        super();
        this.config = config;
        this.objectMapper = objectMapper;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.of(10, ChronoUnit.SECONDS))
                .build();
    }

    @Override
    @Async
    public void tootNewPhoto(Station station, InboxEntry inboxEntry, Photo photo) {
        if (StringUtils.isBlank(config.instanceUrl()) || StringUtils.isBlank(config.token()) || StringUtils.isBlank(config.stationUrl())) {
            log.info("New photo for Station {} not tooted, {}", station.getKey(), this);
            return;
        }
        log.info("Sending toot for new photo of: {}", station.getKey());
        try {
            String json = createStatusJson(station, inboxEntry, photo);
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(config.instanceUrl() + "/api/v1/statuses"))
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
                    .header("Authorization", "Bearer " + config.token())
                    .timeout(Duration.of(30, ChronoUnit.SECONDS))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            var statusCode = response.statusCode();
            var content = response.body();
            if (statusCode >= 200 && statusCode < 300) {
                log.info("Got json response from {}: {}", request.uri(), content);
            } else {
                log.error("Error reading json from {}, status {}: {}", request.uri(), json, content);
            }
        } catch (Exception e) {
            log.error("Error sending Toot", e);
        }
    }

    private String createStatusJson(Station station, InboxEntry inboxEntry, Photo photo) throws JsonProcessingException {
        var status = String.format("%s%nby %s%n%s?countryCode=%s&stationId=%s",
                station.getTitle(), photo.getPhotographer().getDisplayName(), config.stationUrl(),
                station.getKey().getCountry(), station.getKey().getId());
        if (StringUtils.isNotBlank(inboxEntry.getComment())) {
            status += String.format("%n%s", inboxEntry.getComment());
        }
        return objectMapper.writeValueAsString(new Toot(status));
    }

    record Toot(String status) { }

}
