package org.railwaystations.rsapi.adapter.monitoring

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import com.github.tomakehurst.wiremock.matching.MatchResult
import com.github.tomakehurst.wiremock.matching.RequestMatcherExtension
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.URISyntaxException
import java.nio.file.Path
import java.util.*
import javax.imageio.ImageIO

const val ACCESS_TOKEN_PARAM: String = "access_token"
const val FILENAME_PARAM: String = "filename"
const val ANY_ACCESS_TOKEN: String = "accessToken"
const val ROOM_URL_PATH: String = "/roomUrl"
const val UPLOAD_URL_PATH: String = "/uploadUrl"
const val CONTENT_TYPE_HEADER: String = "Content-Type"
const val APPLICATION_JSON_CHARSET_UTF_8: String = "application/json;charset=UTF-8"
const val ANY_FILENAME: String = "test.jpg"
const val ANY_PHOTO_MESSAGE: String = "photoMessage"
const val ANY_TEXT_MESSAGE: String = "textMessage"
const val ANY_CONTENT_URI: String = "/contentUri"

@WireMockTest
internal class MatrixMonitorTest {
    @Test
    fun sendTextMessage(wmRuntimeInfo: WireMockRuntimeInfo) {
        val client = createMatrixMonitor(wmRuntimeInfo)
        WireMock.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(ROOM_URL_PATH))
                .withQueryParam(ACCESS_TOKEN_PARAM, WireMock.equalTo(ANY_ACCESS_TOKEN))
                .willReturn(WireMock.ok())
        )

        client.sendMessage(ANY_TEXT_MESSAGE)

        WireMock.verify(
            WireMock.postRequestedFor(WireMock.urlPathEqualTo(ROOM_URL_PATH))
                .withQueryParam(ACCESS_TOKEN_PARAM, WireMock.equalTo(ANY_ACCESS_TOKEN))
                .withHeader(CONTENT_TYPE_HEADER, WireMock.equalTo(APPLICATION_JSON_CHARSET_UTF_8))
                .withRequestBody(
                    WireMock.equalToJson(
                        """
                        {
                            "body" : "%s",
                            "msgtype" : "m.text"
                        }
                        """.trimIndent().format(ANY_TEXT_MESSAGE)
                    )
                )
        )
    }

    @Test
    @Throws(URISyntaxException::class)
    fun sendPhotoMessage(wmRuntimeInfo: WireMockRuntimeInfo) {
        val client = createMatrixMonitor(wmRuntimeInfo)
        WireMock.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(ROOM_URL_PATH))
                .withQueryParam(ACCESS_TOKEN_PARAM, WireMock.equalTo(ANY_ACCESS_TOKEN))
                .willReturn(WireMock.ok())
        )
        WireMock.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(UPLOAD_URL_PATH))
                .withQueryParam(FILENAME_PARAM, WireMock.equalTo(ANY_FILENAME))
                .withQueryParam(ACCESS_TOKEN_PARAM, WireMock.equalTo(ANY_ACCESS_TOKEN))
                .andMatching(ValidImageContentPattern())
                .willReturn(
                    WireMock.ok(
                        """
                        {"content_uri": "%s"}
                        
                        """.trimIndent().format(ANY_CONTENT_URI)
                    )
                )
        )

        val imagePath = Path.of(Objects.requireNonNull(javaClass.classLoader.getResource(ANY_FILENAME)).toURI())
        client.sendMessage(ANY_PHOTO_MESSAGE, imagePath)

        WireMock.verify(
            WireMock.postRequestedFor(WireMock.urlPathEqualTo(ROOM_URL_PATH))
                .withQueryParam(ACCESS_TOKEN_PARAM, WireMock.equalTo(ANY_ACCESS_TOKEN))
                .withHeader(CONTENT_TYPE_HEADER, WireMock.equalTo(APPLICATION_JSON_CHARSET_UTF_8))
                .withRequestBody(
                    WireMock.equalToJson(
                        """
                        {
                            "body" : "%s",
                            "msgtype" : "m.text"
                        }
                        """.trimIndent().format(ANY_PHOTO_MESSAGE)
                    )
                )
        )

        WireMock.verify(
            WireMock.postRequestedFor(WireMock.urlPathEqualTo(UPLOAD_URL_PATH))
                .withQueryParam(FILENAME_PARAM, WireMock.equalTo(ANY_FILENAME))
                .withQueryParam(ACCESS_TOKEN_PARAM, WireMock.equalTo(ANY_ACCESS_TOKEN))
                .withHeader(CONTENT_TYPE_HEADER, WireMock.equalTo("image/jpeg"))
                .andMatching(ValidImageContentPattern())
        )

        WireMock.verify(
            WireMock.postRequestedFor(WireMock.urlPathEqualTo(ROOM_URL_PATH))
                .withQueryParam(ACCESS_TOKEN_PARAM, WireMock.equalTo(ANY_ACCESS_TOKEN))
                .withHeader(CONTENT_TYPE_HEADER, WireMock.equalTo(APPLICATION_JSON_CHARSET_UTF_8))
                .withRequestBody(
                    WireMock.equalToJson(
                        """
                        {
                            "body": "%s",
                            "url": "%s",
                            "msgtype": "m.image"
                        }
                        """.trimIndent().format(ANY_FILENAME, ANY_CONTENT_URI)
                    )
                )
        )
    }

    private fun createMatrixMonitor(wmRuntimeInfo: WireMockRuntimeInfo): MatrixMonitor {
        val config = MatrixMonitorConfig(
            roomUrl = wmRuntimeInfo.httpBaseUrl + ROOM_URL_PATH,
            uploadUrl = wmRuntimeInfo.httpBaseUrl + UPLOAD_URL_PATH,
            accessToken = ANY_ACCESS_TOKEN
        )
        return MatrixMonitor(config, ObjectMapper())
    }

    internal class ValidImageContentPattern : RequestMatcherExtension() {
        override fun match(request: Request, parameters: Parameters): MatchResult {
            try {
                val image = ImageIO.read(ByteArrayInputStream(request.body))
                    ?: return MatchResult.noMatch()
                return MatchResult.of(image.width <= 300)
            } catch (e: IOException) {
                e.printStackTrace(System.err)
            }
            return MatchResult.noMatch()
        }
    }

}