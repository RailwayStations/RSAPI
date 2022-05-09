package org.railwaystations.rsapi.adapter.out.monitoring;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "matrix")
public record MatrixMonitorConfig(String roomUrl, String uploadUrl, String accessToken) { }
