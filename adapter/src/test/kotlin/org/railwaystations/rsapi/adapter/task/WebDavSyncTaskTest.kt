package org.railwaystations.rsapi.adapter.task

import com.github.tomakehurst.wiremock.client.BasicCredentials
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.badRequest
import com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.created
import com.github.tomakehurst.wiremock.client.WireMock.delete
import com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.noContent
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.request
import com.github.tomakehurst.wiremock.client.WireMock.serverError
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.unauthorized
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.railwaystations.rsapi.adapter.db.InboxDao
import org.railwaystations.rsapi.core.model.InboxEntry
import org.railwaystations.rsapi.core.ports.outbound.PhotoStoragePort
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

const val FILENAME: String = "1.jpg"
const val TO_PROCESS_PATH: String = "/toProcessPath"
const val TO_PROCESS_PATH_FILE: String = "$TO_PROCESS_PATH/$FILENAME"
const val PROCESSED_PATH: String = "/processedPath"
const val PROCESSED_PATH_FILE: String = "$PROCESSED_PATH/$FILENAME"
const val USERNAME: String = "username"
const val PASSWORD: String = "password"
const val PROPFIND_METHOD: String = "PROPFIND"

@WireMockTest
internal class WebDavSyncTaskTest {
    private lateinit var tempdir: Path
    private lateinit var photoStoragePort: PhotoStoragePort
    private lateinit var inboxDao: InboxDao

    @BeforeEach
    fun setup() {
        tempdir = Files.createTempDirectory("rsapi")
        photoStoragePort = mockk<PhotoStoragePort>()
        inboxDao = mockk<InboxDao>()
        every { inboxDao.findPendingInboxEntries() } returns listOf(createInboxEntry())
    }

    @Test
    fun shouldAuthenticate(wmRuntimeInfo: WireMockRuntimeInfo) {
        val task = createWebDavSyncTask(wmRuntimeInfo)
        val toProcessPath = createFile(tempdir)
        every { photoStoragePort.getInboxToProcessFile(FILENAME) } returns toProcessPath
        every { photoStoragePort.getInboxProcessedFile(FILENAME) } returns tempdir.resolve(FILENAME)

        stubFor(
            put(TO_PROCESS_PATH_FILE)
                .willReturn(unauthorized().withHeader("WWW-Authenticate", "Basic realm=\"realm\""))
        )
        stubFor(
            put(TO_PROCESS_PATH_FILE)
                .withBasicAuth(USERNAME, PASSWORD)
                .willReturn(created())
        )
        stubPropfindEmpty()

        task.syncWebDav()

        verify(
            putRequestedFor(urlEqualTo(TO_PROCESS_PATH_FILE))
                .withRequestBody(
                    binaryEqualTo(
                        FILENAME.toByteArray(StandardCharsets.UTF_8)
                    )
                )
        )
        verify(
            putRequestedFor(urlEqualTo(TO_PROCESS_PATH_FILE))
                .withBasicAuth(BasicCredentials(USERNAME, PASSWORD))
                .withRequestBody(
                    binaryEqualTo(
                        FILENAME.toByteArray(StandardCharsets.UTF_8)
                    )
                )
        )
    }

    @Test
    fun shouldUploadToProcessFile(wmRuntimeInfo: WireMockRuntimeInfo) {
        val task = createWebDavSyncTask(wmRuntimeInfo)
        val toProcessPath1 = createFile(tempdir)
        every { photoStoragePort.getInboxToProcessFile(FILENAME) } returns toProcessPath1
        every { photoStoragePort.getInboxProcessedFile(FILENAME) } returns tempdir.resolve(FILENAME)

        stubFor(put(TO_PROCESS_PATH_FILE).willReturn(created()))
        stubPropfindEmpty()

        task.syncWebDav()

        verify(
            putRequestedFor(urlEqualTo(TO_PROCESS_PATH_FILE))
                .withRequestBody(binaryEqualTo(FILENAME.toByteArray(StandardCharsets.UTF_8)))
        )

        Assertions.assertThat(Files.exists(toProcessPath1)).isFalse()
    }

    @Test
    fun shouldKeepFileIfUploadFails(wmRuntimeInfo: WireMockRuntimeInfo) {
        val task = createWebDavSyncTask(wmRuntimeInfo)
        val toProcessPath = createFile(tempdir)
        every { photoStoragePort.getInboxToProcessFile(FILENAME) } returns toProcessPath
        every { photoStoragePort.getInboxProcessedFile(FILENAME) } returns tempdir.resolve(FILENAME)

        stubFor(put(TO_PROCESS_PATH_FILE).willReturn(badRequest()))
        stubPropfindEmpty()

        task.syncWebDav()

        verify(
            putRequestedFor(urlEqualTo(TO_PROCESS_PATH_FILE))
                .withRequestBody(
                    binaryEqualTo(
                        FILENAME.toByteArray(StandardCharsets.UTF_8)
                    )
                )
        )

        Assertions.assertThat(Files.exists(toProcessPath)).isTrue()
    }

    @Test
    fun shouldDownloadProcessedFile(wmRuntimeInfo: WireMockRuntimeInfo) {
        val task = createWebDavSyncTask(wmRuntimeInfo)
        every { photoStoragePort.getInboxToProcessFile(FILENAME) } returns tempdir.resolve(FILENAME)
        val processedPath = tempdir.resolve(FILENAME)
        every { photoStoragePort.getInboxProcessedFile(FILENAME) } returns processedPath

        stubPropfind(createMultistatusResponseForFile())
        stubFor(get(PROCESSED_PATH_FILE).willReturn(ok(FILENAME)))
        stubFor(delete(PROCESSED_PATH_FILE).willReturn(noContent()))

        task.syncWebDav()

        verifyPropfindRequest()
        verify(getRequestedFor(urlEqualTo(PROCESSED_PATH_FILE)))
        verify(deleteRequestedFor(urlEqualTo(PROCESSED_PATH_FILE)))

        Assertions.assertThat(Files.exists(processedPath)).isTrue()
    }

    private fun verifyPropfindRequest() {
        verify(
            RequestPatternBuilder(
                RequestMethod.fromString(PROPFIND_METHOD), urlEqualTo(
                    PROCESSED_PATH
                )
            )
        )
    }

    private fun stubPropfindEmpty() {
        stubPropfind("")
    }

    private fun stubPropfind(multistatusResponse: String) {
        stubFor(
            request(PROPFIND_METHOD, urlPathEqualTo(PROCESSED_PATH)).willReturn(
                aResponse().withStatus(207).withBody(
                    """
                        <?xml version="1.0"?>
                        <d:multistatus xmlns:d="DAV:" xmlns:s="http://sabredav.org/ns" xmlns:oc="http://owncloud.org/ns"
                                       xmlns:nc="http://nextcloud.org/ns">
                            <d:response>
                                <d:href>%s/</d:href>
                                <d:propstat>
                                    <d:prop>
                                        <d:resourcetype>
                                            <d:collection/>
                                        </d:resourcetype>
                                    </d:prop>
                                    <d:status>HTTP/1.1 200 OK</d:status>
                                </d:propstat>
                            </d:response>
                            <d:response>
                                <d:href>%s/Readme.md</d:href>
                                <d:propstat>
                                    <d:prop>
                                        <d:resourcetype/>
                                    </d:prop>
                                    <d:status>HTTP/1.1 200 OK</d:status>
                                </d:propstat>
                            </d:response>
                            %s
                        </d:multistatus>
                        
                        """.trimIndent().format(PROCESSED_PATH, PROCESSED_PATH, multistatusResponse)
                )
            )
        )
    }

    private fun createMultistatusResponseForFile(): String {
        return """
                <d:response>
                    <d:href>%s</d:href>
                    <d:propstat>
                        <d:prop>
                            <d:resourcetype/>
                        </d:prop>
                        <d:status>HTTP/1.1 200 OK</d:status>
                    </d:propstat>
                </d:response>
                
                """.trimIndent().format(PROCESSED_PATH_FILE)
    }

    @Test
    fun shouldNotSendDeleteIfDownloadFailed(wmRuntimeInfo: WireMockRuntimeInfo) {
        val task = createWebDavSyncTask(wmRuntimeInfo)
        every { photoStoragePort.getInboxToProcessFile(FILENAME) } returns tempdir.resolve(FILENAME)
        val processedPath = tempdir.resolve(FILENAME)
        every { photoStoragePort.getInboxProcessedFile(FILENAME) } returns processedPath

        stubPropfind(createMultistatusResponseForFile())
        stubFor(get(PROCESSED_PATH_FILE).willReturn(serverError()))
        stubFor(delete(PROCESSED_PATH_FILE).willReturn(noContent()))

        task.syncWebDav()

        verifyPropfindRequest()
        verify(getRequestedFor(urlEqualTo(PROCESSED_PATH_FILE)))
        verify(0, deleteRequestedFor(urlEqualTo(PROCESSED_PATH_FILE)))

        Assertions.assertThat(Files.exists(processedPath)).isFalse()
    }

    private fun createWebDavSyncTask(wmRuntimeInfo: WireMockRuntimeInfo): WebDavSyncTask {
        val config = WebDavSyncConfig(
            toProcessUrl = wmRuntimeInfo.httpBaseUrl + TO_PROCESS_PATH,
            processedUrl = wmRuntimeInfo.httpBaseUrl + PROCESSED_PATH, user = USERNAME, password = PASSWORD
        )
        return WebDavSyncTask(config, photoStoragePort, inboxDao)
    }

    private fun createInboxEntry(): InboxEntry {
        return InboxEntry(
            id = 1,
            countryCode = "de",
            stationId = "stationId",
            title = "title",
            photographerNickname = "nickname",
            extension = "jpg",
        )
    }

    private fun createFile(dir: Path?): Path {
        val path = dir!!.resolve(FILENAME)
        Files.writeString(path, FILENAME)
        return path
    }

}