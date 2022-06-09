package org.railwaystations.rsapi.adapter.in.task;

import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.railwaystations.rsapi.adapter.out.db.InboxDao;
import org.railwaystations.rsapi.core.model.Coordinates;
import org.railwaystations.rsapi.core.model.InboxEntry;
import org.railwaystations.rsapi.core.ports.out.PhotoStorage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.created;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.request;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.unauthorized;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@WireMockTest
class WebDavSyncTaskTest {

    static final String FILENAME = "1.jpg";
    static final String TO_PROCESS_PATH = "/toProcessPath";
    static final String TO_PROCESS_PATH_FILE = TO_PROCESS_PATH + "/" + FILENAME;
    static final String PROCESSED_PATH = "/processedPath";
    static final String PROCESSED_PATH_FILE = PROCESSED_PATH + "/" + FILENAME;
    static final String USERNAME = "username";
    static final String PASSWORD = "password";
    public static final String PROPFIND_METHOD = "PROPFIND";

    Path tempdir;
    PhotoStorage photoStorage;
    InboxDao inboxDao;

    @BeforeEach
    void setup() throws IOException {
        tempdir = Files.createTempDirectory("rsapi");
        photoStorage = mock(PhotoStorage.class);
        inboxDao = mock(InboxDao.class);
        when(inboxDao.findPendingInboxEntries()).thenReturn(List.of(createInboxEntry()));
    }

    @Test
    void shouldAuthenticate(final WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        final var task = createWebDavSyncTask(wmRuntimeInfo);
        final var toProcessPath = createFile(tempdir);
        when(photoStorage.getInboxToProcessFile(FILENAME)).thenReturn(toProcessPath);
        when(photoStorage.getInboxProcessedFile(FILENAME)).thenReturn(tempdir.resolve(FILENAME));

        stubFor(put(TO_PROCESS_PATH_FILE)
                .willReturn(unauthorized().withHeader("WWW-Authenticate", "Basic realm=\"realm\"")));
        stubFor(put(TO_PROCESS_PATH_FILE)
                .withBasicAuth(USERNAME, PASSWORD)
                .willReturn(created()));
        stubPropfindEmpty();

        task.syncWebDav();

        verify(putRequestedFor(urlEqualTo(TO_PROCESS_PATH_FILE))
                .withRequestBody(binaryEqualTo(FILENAME.getBytes(StandardCharsets.UTF_8)
                )));
        verify(putRequestedFor(urlEqualTo(TO_PROCESS_PATH_FILE))
                .withBasicAuth(new BasicCredentials(USERNAME, PASSWORD))
                .withRequestBody(binaryEqualTo(FILENAME.getBytes(StandardCharsets.UTF_8)
                )));
    }

    @Test
    void shouldUploadToProcessFile(final WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        final var task = createWebDavSyncTask(wmRuntimeInfo);
        final var toProcessPath1 = createFile(tempdir);
        when(photoStorage.getInboxToProcessFile(FILENAME)).thenReturn(toProcessPath1);
        when(photoStorage.getInboxProcessedFile(FILENAME)).thenReturn(tempdir.resolve(FILENAME));

        stubFor(put(TO_PROCESS_PATH_FILE).willReturn(created()));
        stubPropfindEmpty();

        task.syncWebDav();

        verify(putRequestedFor(urlEqualTo(TO_PROCESS_PATH_FILE))
                .withRequestBody(binaryEqualTo(FILENAME.getBytes(StandardCharsets.UTF_8))));

        assertThat(Files.exists(toProcessPath1)).isFalse();
    }

    @Test
    void shouldKeepFileIfUploadFails(final WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        final var task = createWebDavSyncTask(wmRuntimeInfo);
        final var toProcessPath = createFile(tempdir);
        when(photoStorage.getInboxToProcessFile(FILENAME)).thenReturn(toProcessPath);
        when(photoStorage.getInboxProcessedFile(FILENAME)).thenReturn(tempdir.resolve(FILENAME));

        stubFor(put(TO_PROCESS_PATH_FILE).willReturn(badRequest()));
        stubPropfindEmpty();

        task.syncWebDav();

        verify(putRequestedFor(urlEqualTo(TO_PROCESS_PATH_FILE))
                .withRequestBody(binaryEqualTo(FILENAME.getBytes(StandardCharsets.UTF_8)
                )));

        assertThat(Files.exists(toProcessPath)).isTrue();
    }

    @Test
    void shouldDownloadProcessedFile(final WireMockRuntimeInfo wmRuntimeInfo) {
        final var task = createWebDavSyncTask(wmRuntimeInfo);
        when(photoStorage.getInboxToProcessFile(FILENAME)).thenReturn(tempdir.resolve(FILENAME));
        final var processedPath = tempdir.resolve(FILENAME);
        when(photoStorage.getInboxProcessedFile(FILENAME)).thenReturn(processedPath);

        stubPropfind(createMultistatusResponseForFile());
        stubFor(get(PROCESSED_PATH_FILE).willReturn(ok(FILENAME)));
        stubFor(delete(PROCESSED_PATH_FILE).willReturn(noContent()));

        task.syncWebDav();

        verifyPropfindRequest();
        verify(getRequestedFor(urlEqualTo(PROCESSED_PATH_FILE)));
        verify(deleteRequestedFor(urlEqualTo(PROCESSED_PATH_FILE)));

        assertThat(Files.exists(processedPath)).isTrue();
    }

    private void verifyPropfindRequest() {
        verify(new RequestPatternBuilder(RequestMethod.fromString(PROPFIND_METHOD), urlEqualTo(PROCESSED_PATH)));
    }

    private void stubPropfindEmpty() {
        stubPropfind("");
    }

    private void stubPropfind(final String multistatusResponse) {
        stubFor(request(PROPFIND_METHOD, urlPathEqualTo(PROCESSED_PATH)).willReturn(
                aResponse().withStatus(207).withBody("""
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
                        """.formatted(PROCESSED_PATH, PROCESSED_PATH, multistatusResponse))));
    }

    String createMultistatusResponseForFile() {
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
                """.formatted(PROCESSED_PATH_FILE);
    }

    @Test
    void shouldNotSendDeleteIfDownloadFailed(final WireMockRuntimeInfo wmRuntimeInfo) {
        final var task = createWebDavSyncTask(wmRuntimeInfo);
        when(photoStorage.getInboxToProcessFile(FILENAME)).thenReturn(tempdir.resolve(FILENAME));
        final var processedPath = tempdir.resolve(FILENAME);
        when(photoStorage.getInboxProcessedFile(FILENAME)).thenReturn(processedPath);

        stubPropfind(createMultistatusResponseForFile());
        stubFor(get(PROCESSED_PATH_FILE).willReturn(serverError()));
        stubFor(delete(PROCESSED_PATH_FILE).willReturn(noContent()));

        task.syncWebDav();

        verifyPropfindRequest();
        verify(getRequestedFor(urlEqualTo(PROCESSED_PATH_FILE)));
        verify(0, deleteRequestedFor(urlEqualTo(PROCESSED_PATH_FILE)));

        assertThat(Files.exists(processedPath)).isFalse();
    }

    @NotNull
    WebDavSyncTask createWebDavSyncTask(final WireMockRuntimeInfo wmRuntimeInfo) {
        final var config = new WebDavSyncConfig(wmRuntimeInfo.getHttpBaseUrl() + TO_PROCESS_PATH,
                wmRuntimeInfo.getHttpBaseUrl() + PROCESSED_PATH, USERNAME, PASSWORD);
        return new WebDavSyncTask(config, photoStorage, inboxDao);
    }

    @NotNull
    InboxEntry createInboxEntry() {
        return InboxEntry.builder()
                .id(1)
                .countryCode("de")
                .stationId("stationId")
                .title("title")
                .coordinates(new Coordinates())
                .photographerId(1)
                .photographerNickname("nickname")
                .extension("jpg")
                .createdAt(Instant.now())
                .build();
    }

    @NotNull
    Path createFile(final Path dir) throws IOException {
        final var path = dir.resolve(WebDavSyncTaskTest.FILENAME);
        Files.writeString(path, WebDavSyncTaskTest.FILENAME);
        return path;
    }

}