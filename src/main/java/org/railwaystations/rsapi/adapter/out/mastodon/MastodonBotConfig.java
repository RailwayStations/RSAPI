package org.railwaystations.rsapi.adapter.out.mastodon;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mastodon-bot")
public record MastodonBotConfig(String token, String stationUrl, String instanceUrl) { }
