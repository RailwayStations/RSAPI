package org.railwaystations.rsapi.adapter.out.mastodon;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.railwaystations.rsapi.core.model.InboxEntry;
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

    public MastodonBotHttpClient(final MastodonBotConfig config, final ObjectMapper objectMapper) {
        super();
        this.config = config;
        this.objectMapper = objectMapper;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.of(5, ChronoUnit.SECONDS))
                .build();
    }

    @Override
    @Async
    public void tootNewPhoto(final Station station, final InboxEntry inboxEntry) {
        if (StringUtils.isBlank(config.getInstanceUrl()) || StringUtils.isBlank(config.getToken()) || StringUtils.isBlank(config.getStationUrl())) {
            log.info("New photo for Station {} not tooted, {}", station.getKey(), this);
            return;
        }
        log.info("Sending toot for new photo of: {}", station.getKey());
        try {
            final String json = createStatusJson(station, inboxEntry);
            final var request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getInstanceUrl() + "/api/v1/statuses"))
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
                    .header("Authorization", "Bearer " + config.getToken())
                    .timeout(Duration.of(5, ChronoUnit.SECONDS))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            final var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            final var statusCode = response.statusCode();
            final var content = response.body();
            if (statusCode >= 200 && statusCode < 300) {
                log.info("Got json response from {}: {}", request.uri(), content);
            } else {
                log.error("Error reading json from {}, status {}: {}", request.uri(), json, content);
            }
        } catch (final Exception e) {
            log.error("Error sending Toot", e);
        }
    }

    private String createStatusJson(final Station station, final InboxEntry inboxEntry) throws JsonProcessingException {
        var status = String.format("%s%nby %s%n%s?countryCode=%s&stationId=%s",
                station.getTitle(), station.getPhotographer(), config.getStationUrl(),
                station.getKey().getCountry(), station.getKey().getId());
        if (StringUtils.isNotBlank(inboxEntry.getComment())) {
            status += String.format("%n%s", inboxEntry.getComment());
        }
        return objectMapper.writeValueAsString(new Toot(status));
    }

    @Override
    public String toString() {
        return "MastodonBot{" +
                "stationUrl='" + config.getStationUrl() + '\'' +
                ", instanceUrl='" + config.getInstanceUrl() + '\'' +
                '}';
    }

    static class Toot {
        private final String status;

        public Toot(final String status) {
            this.status = status;
        }

        public String getStatus() {
            return status;
        }
    }

}
