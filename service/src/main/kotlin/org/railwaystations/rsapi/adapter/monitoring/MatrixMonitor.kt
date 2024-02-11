package org.railwaystations.rsapi.adapter.monitoring

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.lang3.StringUtils
import org.railwaystations.rsapi.core.ports.Monitor
import org.railwaystations.rsapi.utils.ImageUtil.extensionToMimeType
import org.railwaystations.rsapi.utils.ImageUtil.getExtension
import org.railwaystations.rsapi.utils.ImageUtil.scalePhoto
import org.railwaystations.rsapi.utils.Logger
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import java.time.Duration
import java.time.temporal.ChronoUnit

@Service
@ConditionalOnProperty(prefix = "monitor", name = ["service"], havingValue = "matrix")
class MatrixMonitor(private val config: MatrixMonitorConfig, private val objectMapper: ObjectMapper) : Monitor {

    private val log by Logger()

    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.of(10, ChronoUnit.SECONDS))
        .build()

    @Async
    override fun sendMessage(message: String) {
        sendMessage(message, null)
    }

    @Async
    override fun sendMessage(message: String, file: Path?) {
        log.info("Sending message: {}", message)
        if (StringUtils.isBlank(config.roomUrl)) {
            log.warn("Skipping message, missing Matrix Room URL config")
            return
        }
        try {
            val response = sendRoomMessage(MatrixTextMessage(message))
            val status = response.statusCode()
            val content = response.body()
            if (status in 200..299) {
                log.info("Got json response: {}", content)
            } else {
                log.error("Error reading json, status {}: {}", status, content)
            }

            if (file != null) {
                sendPhoto(file)
            }
        } catch (e: Exception) {
            log.warn("Error sending MatrixMonitor message", e)
        }
    }

    @Throws(Exception::class)
    private fun sendRoomMessage(message: Any): HttpResponse<String> {
        val json = objectMapper.writeValueAsString(message)

        val request = HttpRequest.newBuilder()
            .uri(URI.create(config.roomUrl + "?access_token=" + config.accessToken))
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
            .timeout(Duration.of(30, ChronoUnit.SECONDS))
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build()

        return client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    @Throws(Exception::class)
    private fun sendPhoto(photo: Path) {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(config.uploadUrl + "?filename=" + photo.fileName + "&access_token=" + config.accessToken))
            .header("Content-Type", extensionToMimeType(getExtension(photo.fileName.toString())))
            .timeout(Duration.of(1, ChronoUnit.MINUTES))
            .POST(HttpRequest.BodyPublishers.ofByteArray(scalePhoto(photo, 300)))
            .build()

        val responseUpload = client.send(request, HttpResponse.BodyHandlers.ofString())
        val statusUpload = responseUpload.statusCode()
        val contentUpload = responseUpload.body()
        if (statusUpload in 200..299) {
            log.info("Got json response: {}", contentUpload)
        } else {
            log.error("Error reading json, statusUpload {}: {}", statusUpload, contentUpload)
            return
        }

        val matrixUploadResponse = objectMapper.readValue(contentUpload, MatrixUploadResponse::class.java)

        val responseImage =
            sendRoomMessage(MatrixImageMessage(photo.fileName.toString(), matrixUploadResponse.contentUri))
        val statusImage = responseImage.statusCode()
        val contentImage = responseImage.body()
        if (statusImage in 200..299) {
            log.info("Got json response: {}", contentImage)
        } else {
            log.error("Error reading json, statusUpload {}: {}", statusImage, contentImage)
        }
    }

    data class MatrixTextMessage(val body: String?) {
        val msgtype: String
            get() = "m.text"
    }

    data class MatrixImageMessage(val body: String, val url: String) {
        val msgtype: String
            get() = "m.image"

    }

    data class MatrixUploadResponse(@JsonProperty("content_uri") val contentUri: String)
}
