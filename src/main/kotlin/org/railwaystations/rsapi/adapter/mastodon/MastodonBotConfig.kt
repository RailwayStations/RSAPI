package org.railwaystations.rsapi.adapter.mastodon

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "mastodon-bot")
data class MastodonBotConfig(val token: String, val stationUrl: String, val instanceUrl: String)
