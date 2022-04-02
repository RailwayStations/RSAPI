package org.railwaystations.rsapi.adapter.in.web.controller;

import org.railwaystations.rsapi.adapter.out.mastodon.MastodonBotConfig;
import org.railwaystations.rsapi.adapter.out.mastodon.MastodonBotHttpClient;
import org.railwaystations.rsapi.adapter.out.monitoring.MockMonitor;
import org.railwaystations.rsapi.adapter.out.photostorage.WorkDir;
import org.railwaystations.rsapi.core.ports.out.MastodonBot;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.nio.file.Files;

@TestConfiguration
@Profile({ "mockMvcTest" })
public class MockMvcTestConfiguration {

    @Bean
    MastodonBot createMastodonBot() {
        return new MastodonBotHttpClient(new MastodonBotConfig());
    }

    @Bean
    WorkDir createWorkDir() throws IOException {
        return new WorkDir(Files.createTempDirectory("rsapi").toString(), null);
    }

    @Bean
    MockMonitor createMonitor() {
        return new MockMonitor();
    }

}
