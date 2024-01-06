package org.railwaystations.rsapi.adapter.web.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.railwaystations.rsapi.adapter.mastodon.MastodonBotConfig
import org.railwaystations.rsapi.adapter.mastodon.MastodonBotHttpClient
import org.railwaystations.rsapi.adapter.monitoring.FakeMonitor
import org.railwaystations.rsapi.adapter.photostorage.WorkDir
import org.railwaystations.rsapi.core.ports.MastodonBot
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import java.nio.file.Files

@TestConfiguration
@Profile("mockMvcTest")
class MockMvcTestConfiguration {
    @Bean
    fun mastodonBot(objectMapper: ObjectMapper): MastodonBot {
        return MastodonBotHttpClient(MastodonBotConfig("", "", ""), objectMapper)
    }

    @Bean
    fun workDir(): WorkDir {
        return WorkDir(Files.createTempDirectory("rsapi").toString(), null)
    }

    @Bean
    fun monitor(): FakeMonitor {
        return FakeMonitor()
    }
}
