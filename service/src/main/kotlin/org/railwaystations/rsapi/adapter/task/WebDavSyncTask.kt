package org.railwaystations.rsapi.adapter.task

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.railwaystations.rsapi.adapter.db.InboxDao
import org.railwaystations.rsapi.core.model.InboxEntry
import org.railwaystations.rsapi.core.ports.PhotoStorage
import org.railwaystations.rsapi.core.utils.Logger
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.io.IOException
import java.net.Authenticator
import java.net.PasswordAuthentication
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.temporal.ChronoUnit

@Component
@ConditionalOnProperty(prefix = "webdavsync", name = ["enabled"], havingValue = "true")
class WebDavSyncTask(
    private val config: WebDavSyncConfig,
    private val photoStorage: PhotoStorage,
    private val inboxDao: InboxDao,
) {

    private val log by Logger()

    private val xmlMapper = XmlMapper()

    init {
        xmlMapper.registerModule(
            KotlinModule.Builder().build()
        )
    }


    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.of(10, ChronoUnit.SECONDS))
        .authenticator(object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(config.user, config.password.toCharArray())
            }
        })
        .build()

    @Scheduled(fixedRate = 60000)
    fun syncWebDav() {
        log.info("Starting WebDavSync")
        val pendingInboxEntries: List<InboxEntry> = inboxDao.findPendingInboxEntries()
        if (pendingInboxEntries.isEmpty()) {
            return  // nothing to do
        }

        val processedFiles = listProcessedFiles()
        pendingInboxEntries.filter { entry -> entry.isPhotoUpload }
            .forEach { inboxEntry: InboxEntry -> checkWebDav(inboxEntry, processedFiles) }
    }

    private fun listProcessedFiles(): List<MultistatusResponse> {
        log.info("ListProcessedFiles")
        val request = HttpRequest.newBuilder()
            .uri(URI.create(config.processedUrl))
            .timeout(Duration.of(1, ChronoUnit.MINUTES))
            .method(
                "PROPFIND", HttpRequest.BodyPublishers.ofString(
                    """
                        <?xml version="1.0"?>
                        <a:propfind xmlns:a="DAV:">
                        <a:prop><a:resourcetype/></a:prop>
                        </a:propfind>
                        """.trimIndent()
                )
            )
            .header("Depth", "1")
            .build()
        try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            log.info("ListProcessedFiles response " + response.statusCode())
            if (response.statusCode() != HttpStatus.MULTI_STATUS.value()) {
                throw RuntimeException("Failed to list processed files, statusCode=" + response.statusCode())
            }
            return xmlMapper.readValue(response.body(), Multistatus::class.java).responses
        } catch (e: IOException) {
            throw RuntimeException("Failed to list processed files", e)
        } catch (e: InterruptedException) {
            throw RuntimeException("Failed to list processed files", e)
        }
    }

    private fun checkWebDav(inboxEntry: InboxEntry, processedFiles: List<MultistatusResponse>) {
        val toProcessPath = photoStorage.getInboxToProcessFile(inboxEntry.filename!!)
        if (Files.exists(toProcessPath)) {
            try {
                uploadToProcess(toProcessPath)
                Files.delete(toProcessPath)
            } catch (e: Exception) {
                log.error("Unable to upload toProcess {}", toProcessPath, e)
            }
        }
        val processedPath = photoStorage.getInboxProcessedFile(inboxEntry.filename!!)
        if (checkIfDownloadProcessedNeeded(processedPath, processedFiles)) {
            try {
                downloadProcessed(processedPath)
            } catch (e: Exception) {
                log.error("Unable to download of {}", processedPath, e)
            }
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun downloadProcessed(processedPath: Path) {
        log.info("Downloading processed file of {}", processedPath)
        val processedUri = URI.create(config.processedUrl + "/" + processedPath.fileName.toString())
        val getRequest = HttpRequest.newBuilder()
            .uri(processedUri)
            .timeout(Duration.of(1, ChronoUnit.MINUTES))
            .GET()
            .build()
        val getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofFile(processedPath))
        log.info(
            "Download getResponse {}, bytes {}",
            getResponse.statusCode(),
            Files.size(processedPath)
        )

        if (getResponse.statusCode() == HttpStatus.OK.value()) {
            val delRequest = HttpRequest.newBuilder()
                .uri(processedUri)
                .timeout(Duration.of(1, ChronoUnit.MINUTES))
                .DELETE()
                .build()
            val delResponse = client.send(delRequest, HttpResponse.BodyHandlers.ofFile(processedPath))
            log.info("Deleted {}, status {}", delRequest.uri(), delResponse.statusCode())
        } else {
            Files.deleteIfExists(processedPath)
        }
    }

    private fun checkIfDownloadProcessedNeeded(
        processedPath: Path,
        processedFiles: List<MultistatusResponse>
    ): Boolean {
        return processedFiles.any { multistatusResponse ->
            multistatusResponse.href.endsWith(processedPath.fileName.toString())
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun uploadToProcess(toProcessPath: Path) {
        log.info("Uploading $toProcessPath")
        val request = HttpRequest.newBuilder()
            .uri(URI.create(config.toProcessUrl + "/" + toProcessPath.fileName.toString()))
            .timeout(Duration.of(1, ChronoUnit.MINUTES))
            .PUT(HttpRequest.BodyPublishers.ofFile(toProcessPath))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        log.info("Upload response " + response.statusCode())
        if (response.statusCode() != HttpStatus.CREATED.value()) {
            throw RuntimeException("Failed Upload, statusCode=" + response.statusCode())
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Multistatus(
        @JacksonXmlElementWrapper(useWrapping = false)
        @JsonProperty("response")
        var responses: List<MultistatusResponse>
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class MultistatusResponse(
        var href: String
    )
}
