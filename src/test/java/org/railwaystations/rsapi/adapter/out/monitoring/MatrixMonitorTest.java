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

    @Test
    void sendTextMessage(final WireMockRuntimeInfo wmRuntimeInfo) {
        final var client = createMatrixMonitor(wmRuntimeInfo);
        stubFor(post(urlPathEqualTo("/roomUrl"))
                .withQueryParam("access_token", equalTo("accessToken"))
                .willReturn(ok()));

        client.sendMessage("textMessage");

        verify(postRequestedFor(urlPathEqualTo("/roomUrl"))
                .withQueryParam("access_token", equalTo("accessToken"))
                .withHeader("Content-Type", equalTo("application/json;charset=UTF-8"))
                .withRequestBody(equalToJson("""
                        {
                            "body" : "textMessage",
                            "msgtype" : "m.text"
                        }"""
                )));
    }


    @Test
    void sendPhotoMessage(final WireMockRuntimeInfo wmRuntimeInfo) throws URISyntaxException {
        final var client = createMatrixMonitor(wmRuntimeInfo);
        stubFor(post(urlPathEqualTo("/roomUrl"))
                .withQueryParam("access_token", equalTo("accessToken"))
                .willReturn(ok()));
        stubFor(post(urlPathEqualTo("/uploadUrl"))
                .withQueryParam("filename", equalTo("test.jpg"))
                .withQueryParam("access_token", equalTo("accessToken"))
                .andMatching(new ValidImageContentPattern())
                .willReturn(ok("{\"content_uri\": \"/contentUri\"}")));

        final var imagePath = Path.of(getClass().getClassLoader().getResource("test.jpg").toURI());
        client.sendMessage("photoMessage", imagePath);

        verify(postRequestedFor(urlPathEqualTo("/roomUrl"))
                .withQueryParam("access_token", equalTo("accessToken"))
                .withHeader("Content-Type", equalTo("application/json;charset=UTF-8"))
                .withRequestBody(equalToJson("""
                        {
                            "body" : "photoMessage",
                            "msgtype" : "m.text"
                        }"""
                )));

        verify(postRequestedFor(urlPathEqualTo("/uploadUrl"))
                .withQueryParam("filename", equalTo("test.jpg"))
                .withQueryParam("access_token", equalTo("accessToken"))
                .withHeader("Content-Type", equalTo("image/jpeg"))
                .andMatching(new ValidImageContentPattern()));

        verify(postRequestedFor(urlPathEqualTo("/roomUrl"))
                .withQueryParam("access_token", equalTo("accessToken"))
                .withHeader("Content-Type", equalTo("application/json;charset=UTF-8"))
                .withRequestBody(equalToJson("""
                        {
                            "body": "test.jpg",
                            "url": "/contentUri",
                            "msgtype": "m.image"
                        }"""
                )));
    }

    @NotNull
    private MatrixMonitor createMatrixMonitor(final WireMockRuntimeInfo wmRuntimeInfo) {
        final var config = new MatrixMonitorConfig(wmRuntimeInfo.getHttpBaseUrl() + "/roomUrl", wmRuntimeInfo.getHttpBaseUrl() + "/uploadUrl", "accessToken");
        return new MatrixMonitor(config, new ObjectMapper());
    }

    private static class ValidImageContentPattern extends RequestMatcherExtension {
        @Override
        public MatchResult match(final Request request, final Parameters parameters) {
            try {
                final var image = ImageIO.read(new ByteArrayInputStream(request.getBody()));
                if (image == null) {
                    return MatchResult.noMatch();
                }
                return MatchResult.of(image.getWidth() <= 300);
            } catch (final IOException e) {
                e.printStackTrace(System.err);
            }
            return MatchResult.noMatch();
        }
    }

}