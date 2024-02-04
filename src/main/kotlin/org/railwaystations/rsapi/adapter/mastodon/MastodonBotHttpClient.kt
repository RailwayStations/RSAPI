package org.railwaystations.rsapi.adapter.mastodon

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.lang3.StringUtils
import org.railwaystations.rsapi.core.ports.MastodonBot
import org.railwaystations.rsapi.utils.Logger
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.temporal.ChronoUnit

@Service
class MastodonBotHttpClient(
    private val config: MastodonBotConfig,
    private val objectMapper: ObjectMapper,
) : MastodonBot {

    private val log by Logger()

    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.of(10, ChronoUnit.SECONDS))
        .build()

    @Async
    override fun tootNewPhoto(status: String) {
        if (StringUtils.isBlank(config.instanceUrl) || StringUtils.isBlank(
                config.token
            )
        ) {
            log.info("New photo not tooted: {}", status)
            return
        }
        log.info("Sending toot for new photo: {}", status)
        try {
            val json = objectMapper.writeValueAsString(Toot(status))
            val request = HttpRequest.newBuilder()
                .uri(URI.create(config.instanceUrl + "/api/v1/statuses"))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
                .header("Authorization", "Bearer " + config.token)
                .timeout(Duration.of(30, ChronoUnit.SECONDS))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            val statusCode = response.statusCode()
            val content = response.body()
            if (statusCode in 200..299) {
                log.info("Got json response from {}: {}", request.uri(), content)
            } else {
                log.error(
                    "Error reading json from {}, status {}: {}",
                    request.uri(),
                    json,
                    content
                )
            }
        } catch (e: Exception) {
            log.error("Error sending Toot", e)
        }
    }

    data class Toot(val status: String)
}
