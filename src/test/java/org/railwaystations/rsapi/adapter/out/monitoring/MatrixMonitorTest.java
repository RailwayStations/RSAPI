package org.railwaystations.rsapi.adapter.out.monitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.RequestMatcherExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

@WireMockTest
class MatrixMonitorTest {

    static final String ACCESS_TOKEN_PARAM = "access_token";
    static final String FILENAME_PARAM = "filename";
    static final String ANY_ACCESS_TOKEN = "accessToken";
    static final String ROOM_URL_PATH = "/roomUrl";
    static final String UPLOAD_URL_PATH = "/uploadUrl";
    static final String CONTENT_TYPE_HEADER = "Content-Type";
    static final String APPLICATION_JSON_CHARSET_UTF_8 = "application/json;charset=UTF-8";
    static final String ANY_FILENAME = "test.jpg";
    static final String ANY_PHOTO_MESSAGE = "photoMessage";
    static final String ANY_TEXT_MESSAGE = "textMessage";
    static final String ANY_CONTENT_URI = "/contentUri";

    @Test
    void sendTextMessage(WireMockRuntimeInfo wmRuntimeInfo) {
        var client = createMatrixMonitor(wmRuntimeInfo);
        stubFor(post(urlPathEqualTo(ROOM_URL_PATH))
                .withQueryParam(ACCESS_TOKEN_PARAM, equalTo(ANY_ACCESS_TOKEN))
                .willReturn(ok()));

        client.sendMessage(ANY_TEXT_MESSAGE);

        verify(postRequestedFor(urlPathEqualTo(ROOM_URL_PATH))
                .withQueryParam(ACCESS_TOKEN_PARAM, equalTo(ANY_ACCESS_TOKEN))
                .withHeader(CONTENT_TYPE_HEADER, equalTo(APPLICATION_JSON_CHARSET_UTF_8))
                .withRequestBody(equalToJson("""
                        {
                            "body" : "%s",
                            "msgtype" : "m.text"
                        }""".formatted(ANY_TEXT_MESSAGE)
                )));
    }


    @Test
    void sendPhotoMessage(WireMockRuntimeInfo wmRuntimeInfo) throws URISyntaxException {
        var client = createMatrixMonitor(wmRuntimeInfo);
        stubFor(post(urlPathEqualTo(ROOM_URL_PATH))
                .withQueryParam(ACCESS_TOKEN_PARAM, equalTo(ANY_ACCESS_TOKEN))
                .willReturn(ok()));
        stubFor(post(urlPathEqualTo(UPLOAD_URL_PATH))
                .withQueryParam(FILENAME_PARAM, equalTo(ANY_FILENAME))
                .withQueryParam(ACCESS_TOKEN_PARAM, equalTo(ANY_ACCESS_TOKEN))
                .andMatching(new ValidImageContentPattern())
                .willReturn(ok("""
                            {"content_uri": "%s"}
                            """.formatted(ANY_CONTENT_URI)
                )));

        var imagePath = Path.of(Objects.requireNonNull(getClass().getClassLoader().getResource(ANY_FILENAME)).toURI());
        client.sendMessage(ANY_PHOTO_MESSAGE, imagePath);

        verify(postRequestedFor(urlPathEqualTo(ROOM_URL_PATH))
                .withQueryParam(ACCESS_TOKEN_PARAM, equalTo(ANY_ACCESS_TOKEN))
                .withHeader(CONTENT_TYPE_HEADER, equalTo(APPLICATION_JSON_CHARSET_UTF_8))
                .withRequestBody(equalToJson("""
                        {
                            "body" : "%s",
                            "msgtype" : "m.text"
                        }""".formatted(ANY_PHOTO_MESSAGE)
                )));

        verify(postRequestedFor(urlPathEqualTo(UPLOAD_URL_PATH))
                .withQueryParam(FILENAME_PARAM, equalTo(ANY_FILENAME))
                .withQueryParam(ACCESS_TOKEN_PARAM, equalTo(ANY_ACCESS_TOKEN))
                .withHeader(CONTENT_TYPE_HEADER, equalTo("image/jpeg"))
                .andMatching(new ValidImageContentPattern()));

        verify(postRequestedFor(urlPathEqualTo(ROOM_URL_PATH))
                .withQueryParam(ACCESS_TOKEN_PARAM, equalTo(ANY_ACCESS_TOKEN))
                .withHeader(CONTENT_TYPE_HEADER, equalTo(APPLICATION_JSON_CHARSET_UTF_8))
                .withRequestBody(equalToJson("""
                        {
                            "body": "%s",
                            "url": "%s",
                            "msgtype": "m.image"
                        }""".formatted(ANY_FILENAME, ANY_CONTENT_URI)
                )));
    }

    @NotNull
    MatrixMonitor createMatrixMonitor(WireMockRuntimeInfo wmRuntimeInfo) {
        var config = new MatrixMonitorConfig(wmRuntimeInfo.getHttpBaseUrl() + ROOM_URL_PATH, wmRuntimeInfo.getHttpBaseUrl() + UPLOAD_URL_PATH, ANY_ACCESS_TOKEN);
        return new MatrixMonitor(config, new ObjectMapper());
    }

    static class ValidImageContentPattern extends RequestMatcherExtension {
        @Override
        public MatchResult match(Request request, Parameters parameters) {
            try {
                var image = ImageIO.read(new ByteArrayInputStream(request.getBody()));
                if (image == null) {
                    return MatchResult.noMatch();
                }
                return MatchResult.of(image.getWidth() <= 300);
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
            return MatchResult.noMatch();
        }
    }

}