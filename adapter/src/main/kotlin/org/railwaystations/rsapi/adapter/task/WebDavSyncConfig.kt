package org.railwaystations.rsapi.adapter.task

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "webdavsync")
data class WebDavSyncConfig(val toProcessUrl: String, val processedUrl: String, val user: String, val password: String)
