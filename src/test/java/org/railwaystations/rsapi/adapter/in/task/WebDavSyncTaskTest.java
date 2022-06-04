package org.railwaystations.rsapi.adapter.in.task;

import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import org.jetbrains.annotations.NotNull;
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

import static com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo;
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
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.unauthorized;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@WireMockTest
class WebDavSyncTaskTest {

    public static final String FILENAME_1 = "1.jpg";
    public static final String PROCESSED_PATH_FILE_1 = "/processedPath/" + FILENAME_1;
    public static final String TO_PROCESS_PATH_FILE_1 = "/toProcessPath/" + FILENAME_1;
    public static final String FILENAME_2 = "2.jpg";
    public static final String PROCESSED_PATH_FILE_2 = "/processedPath/" + FILENAME_2;
    public static final String TO_PROCESS_PATH = "/toProcessPath";
    public static final String PROCESSED_PATH = "/processedPath";

    @Test
    void syncWebDav(final WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        final var tempdir = Files.createTempDirectory("rsapi");
        final var config = new WebDavSyncConfig(wmRuntimeInfo.getHttpBaseUrl() + TO_PROCESS_PATH, wmRuntimeInfo.getHttpBaseUrl() + PROCESSED_PATH, "username", "password");
        final var photoStorage = mock(PhotoStorage.class);
        final var inboxDao = mock(InboxDao.class);
        final var task = new WebDavSyncTask(config, photoStorage, inboxDao);
        when(inboxDao.findPendingInboxEntries()).thenReturn(List.of(createInboxEntry(1L),createInboxEntry(2L)));
        final var toProcessPath1 = createFile1(tempdir);
        when(photoStorage.getInboxToProcessFile(FILENAME_1)).thenReturn(toProcessPath1);
        when(photoStorage.getInboxToProcessFile(FILENAME_2)).thenReturn(tempdir.resolve(FILENAME_2));
        when(photoStorage.getInboxProcessedFile(FILENAME_1)).thenReturn(tempdir.resolve(FILENAME_1));
        final var processedPath2 = tempdir.resolve(FILENAME_2);
        when(photoStorage.getInboxProcessedFile(FILENAME_2)).thenReturn(processedPath2);

        stubFor(put(TO_PROCESS_PATH_FILE_1).willReturn(unauthorized().withHeader("WWW-Authenticate", "Basic realm=\"realm\"")));
        stubFor(put(TO_PROCESS_PATH_FILE_1)
                .withBasicAuth(config.user(), config.password())
                .willReturn(ok()));
        stubFor(head(new UrlPattern(equalTo(PROCESSED_PATH_FILE_1), false)).willReturn(notFound()));
        stubFor(head(new UrlPattern(equalTo(PROCESSED_PATH_FILE_2), false)).willReturn(ok()));
        stubFor(get(PROCESSED_PATH_FILE_2).willReturn(ok(FILENAME_2)));
        stubFor(delete(PROCESSED_PATH_FILE_2).willReturn(noContent()));

        task.syncWebDav();

        verify(putRequestedFor(urlEqualTo(TO_PROCESS_PATH_FILE_1))
                .withRequestBody(binaryEqualTo(FILENAME_1.getBytes(StandardCharsets.UTF_8)
                )));
        verify(putRequestedFor(urlEqualTo(TO_PROCESS_PATH_FILE_1))
                .withBasicAuth(new BasicCredentials(config.user(), config.password()))
                .withRequestBody(binaryEqualTo(FILENAME_1.getBytes(StandardCharsets.UTF_8)
                )));
        verify(headRequestedFor(urlEqualTo(PROCESSED_PATH_FILE_1)));
        verify(headRequestedFor(urlEqualTo(PROCESSED_PATH_FILE_2)));
        verify(getRequestedFor(urlEqualTo(PROCESSED_PATH_FILE_2)));
        verify(deleteRequestedFor(urlEqualTo(PROCESSED_PATH_FILE_2)));

        assertThat(Files.exists(toProcessPath1)).isFalse();
        assertThat(Files.exists(processedPath2)).isTrue();
    }

    @NotNull
    private InboxEntry createInboxEntry(final long id) {
        return new InboxEntry(id, "de", "stationId", "title", new Coordinates(), 1, "nickname", null,
                "jpg", null, null, Instant.now(), false, null, false, false, null, true, 0L, false);
    }

    @NotNull
    private Path createFile1(final Path dir) throws IOException {
        final var path = dir.resolve(WebDavSyncTaskTest.FILENAME_1);
        Files.writeString(path, WebDavSyncTaskTest.FILENAME_1);
        return path;
    }

}