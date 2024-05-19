package org.railwaystations.rsapi.adapter.monitoring

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "matrix")
data class MatrixMonitorConfig(val roomUrl: String, val uploadUrl: String, val accessToken: String)
