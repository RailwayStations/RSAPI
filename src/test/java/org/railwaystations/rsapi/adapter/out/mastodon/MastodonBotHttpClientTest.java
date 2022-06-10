package org.railwaystations.rsapi.adapter.out.mastodon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.railwaystations.rsapi.core.model.Coordinates;
import org.railwaystations.rsapi.core.model.InboxEntry;
import org.railwaystations.rsapi.core.model.Photo;
import org.railwaystations.rsapi.core.model.Station;
import org.railwaystations.rsapi.core.model.User;

import java.time.Instant;

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

        var key = new Station.Key("de", "1234");
        var user = new User(0, "name", "url", User.CC0, "email", true, false, "key", false, null, null, null, true);
        var photo = new Photo(key, "urlPath", user, Instant.now(), User.CC0);
        var station = new Station(key, "title", new Coordinates(), photo, true);
        var inboxEntry = InboxEntry.builder()
                        .comment("comment")
                        .build();
        client.tootNewPhoto(station, inboxEntry);

        verify(postRequestedFor(urlEqualTo("/api/v1/statuses"))
                .withHeader("Authorization", equalTo("Bearer token"))
                .withHeader("Content-Type", equalTo("application/json;charset=UTF-8"))
                .withRequestBody(equalToJson("""
                        {"status": "title\\nby name\\nhttps://station.url?countryCode=de&stationId=1234\\ncomment"}"""
                )));
    }
}