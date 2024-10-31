package org.railwaystations.rsapi.adapter.mastodon

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import org.junit.jupiter.api.Test

@WireMockTest
internal class MastodonPortHttpClientTest {
    @Test
    fun tootNewPhoto(wmRuntimeInfo: WireMockRuntimeInfo) {
        val config = MastodonBotConfig("token", "https://station.url", wmRuntimeInfo.httpBaseUrl)
        val client = MastodonBotHttpClient(config, ObjectMapper())
        WireMock.stubFor(
            WireMock.post("/api/v1/statuses")
                .willReturn(WireMock.ok())
        )

        client.postPhoto("status message")

        WireMock.verify(
            WireMock.postRequestedFor(WireMock.urlEqualTo("/api/v1/statuses"))
                .withHeader("Authorization", WireMock.equalTo("Bearer token"))
                .withHeader("Content-Type", WireMock.equalTo("application/json;charset=UTF-8"))
                .withRequestBody(
                    WireMock.equalToJson(
                        """
                        {"status": "status message", "visibility" : "unlisted"}
                        """.trimIndent()
                    )
                )
        )
    }
}