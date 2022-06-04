package org.railwaystations.rsapi.adapter.in.task;

import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
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

import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.created;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.headRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.unauthorized;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
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
    void should_authenticate(final WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        final var task = createWebDavSyncTask(wmRuntimeInfo);
        final var toProcessPath = createFile(tempdir);
        when(photoStorage.getInboxToProcessFile(FILENAME)).thenReturn(toProcessPath);
        when(photoStorage.getInboxProcessedFile(FILENAME)).thenReturn(tempdir.resolve(FILENAME));

        stubFor(put(TO_PROCESS_PATH_FILE)
                .willReturn(unauthorized().withHeader("WWW-Authenticate", "Basic realm=\"realm\"")));
        stubFor(put(TO_PROCESS_PATH_FILE)
                .withBasicAuth(USERNAME, PASSWORD)
                .willReturn(created()));
        stubFor(head(new UrlPattern(equalTo(PROCESSED_PATH_FILE), false)).willReturn(notFound()));

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
    void should_upload_to_process_file(final WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        final var task = createWebDavSyncTask(wmRuntimeInfo);
        final var toProcessPath1 = createFile(tempdir);
        when(photoStorage.getInboxToProcessFile(FILENAME)).thenReturn(toProcessPath1);
        when(photoStorage.getInboxProcessedFile(FILENAME)).thenReturn(tempdir.resolve(FILENAME));

        stubFor(put(TO_PROCESS_PATH_FILE).willReturn(created()));
        stubFor(head(new UrlPattern(equalTo(PROCESSED_PATH_FILE), false)).willReturn(notFound()));

        task.syncWebDav();

        verify(putRequestedFor(urlEqualTo(TO_PROCESS_PATH_FILE))
                .withRequestBody(binaryEqualTo(FILENAME.getBytes(StandardCharsets.UTF_8))));
        verify(headRequestedFor(urlEqualTo(PROCESSED_PATH_FILE)));

        assertThat(Files.exists(toProcessPath1)).isFalse();
    }

    @Test
    void should_keep_file_if_upload_fails(final WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        final var task = createWebDavSyncTask(wmRuntimeInfo);
        final var toProcessPath = createFile(tempdir);
        when(photoStorage.getInboxToProcessFile(FILENAME)).thenReturn(toProcessPath);
        when(photoStorage.getInboxProcessedFile(FILENAME)).thenReturn(tempdir.resolve(FILENAME));

        stubFor(put(TO_PROCESS_PATH_FILE).willReturn(badRequest()));
        stubFor(head(new UrlPattern(equalTo(PROCESSED_PATH_FILE), false)).willReturn(notFound()));

        task.syncWebDav();

        verify(putRequestedFor(urlEqualTo(TO_PROCESS_PATH_FILE))
                .withRequestBody(binaryEqualTo(FILENAME.getBytes(StandardCharsets.UTF_8)
                )));

        assertThat(Files.exists(toProcessPath)).isTrue();
    }

    @Test
    void should_download_processed_file(final WireMockRuntimeInfo wmRuntimeInfo) {
        final var task = createWebDavSyncTask(wmRuntimeInfo);
        when(photoStorage.getInboxToProcessFile(FILENAME)).thenReturn(tempdir.resolve(FILENAME));
        final var processedPath = tempdir.resolve(FILENAME);
        when(photoStorage.getInboxProcessedFile(FILENAME)).thenReturn(processedPath);

        stubFor(head(new UrlPattern(equalTo(PROCESSED_PATH_FILE), false)).willReturn(ok()));
        stubFor(get(PROCESSED_PATH_FILE).willReturn(ok(FILENAME)));
        stubFor(delete(PROCESSED_PATH_FILE).willReturn(noContent()));

        task.syncWebDav();

        verify(headRequestedFor(urlEqualTo(PROCESSED_PATH_FILE)));
        verify(getRequestedFor(urlEqualTo(PROCESSED_PATH_FILE)));
        verify(deleteRequestedFor(urlEqualTo(PROCESSED_PATH_FILE)));

        assertThat(Files.exists(processedPath)).isTrue();
    }

    @Test
    void should_not_send_delete_if_download_failed(final WireMockRuntimeInfo wmRuntimeInfo) {
        final var task = createWebDavSyncTask(wmRuntimeInfo);
        when(photoStorage.getInboxToProcessFile(FILENAME)).thenReturn(tempdir.resolve(FILENAME));
        final var processedPath = tempdir.resolve(FILENAME);
        when(photoStorage.getInboxProcessedFile(FILENAME)).thenReturn(processedPath);

        stubFor(head(new UrlPattern(equalTo(PROCESSED_PATH_FILE), false)).willReturn(ok()));
        stubFor(get(PROCESSED_PATH_FILE).willReturn(serverError()));
        stubFor(delete(PROCESSED_PATH_FILE).willReturn(noContent()));

        task.syncWebDav();

        verify(headRequestedFor(urlEqualTo(PROCESSED_PATH_FILE)));
        verify(getRequestedFor(urlEqualTo(PROCESSED_PATH_FILE)));
        verify(0, deleteRequestedFor(urlEqualTo(PROCESSED_PATH_FILE)));

        assertThat(Files.exists(processedPath)).isFalse();
    }

    @NotNull
    private WebDavSyncTask createWebDavSyncTask(final WireMockRuntimeInfo wmRuntimeInfo) {
        final var config = new WebDavSyncConfig(wmRuntimeInfo.getHttpBaseUrl() + TO_PROCESS_PATH,
                wmRuntimeInfo.getHttpBaseUrl() + PROCESSED_PATH, USERNAME, PASSWORD);
        return new WebDavSyncTask(config, photoStorage, inboxDao);
    }

    @NotNull
    private InboxEntry createInboxEntry() {
        return new InboxEntry(1, "de", "stationId", "title", new Coordinates(),
                1, "nickname", null,
                "jpg", null, null, Instant.now(),
                false, null, false, false,
                null, true, 0L, false);
    }

    @NotNull
    private Path createFile(final Path dir) throws IOException {
        final var path = dir.resolve(WebDavSyncTaskTest.FILENAME);
        Files.writeString(path, WebDavSyncTaskTest.FILENAME);
        return path;
    }

}