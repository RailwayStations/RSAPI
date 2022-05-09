package org.railwaystations.rsapi.adapter.in.task;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "webdavsync")
public record WebDavSyncConfig(String toProcessUrl, String processedUrl, String user, String password) { }
