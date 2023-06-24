package org.railwaystations.rsapi.adapter.out.mastodon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

@WireMockTest
class MastodonBotHttpClientTest {

    @Test
    void tootNewPhoto(WireMockRuntimeInfo wmRuntimeInfo) {
        var config = new MastodonBotConfig("token", "https://station.url", wmRuntimeInfo.getHttpBaseUrl());
        var client = new MastodonBotHttpClient(config, new ObjectMapper());
        stubFor(post("/api/v1/statuses")
                .willReturn(ok()));

        client.tootNewPhoto("status message");

        verify(postRequestedFor(urlEqualTo("/api/v1/statuses"))
                .withHeader("Authorization", equalTo("Bearer token"))
                .withHeader("Content-Type", equalTo("application/json;charset=UTF-8"))
                .withRequestBody(equalToJson("""
                        {"status": "status message"}"""
                )));
    }
}