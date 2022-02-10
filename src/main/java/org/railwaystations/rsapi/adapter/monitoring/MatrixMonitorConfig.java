package org.railwaystations.rsapi.adapter.monitoring;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "matrix")
public class MatrixMonitorConfig {

    private String roomUrl;

    private String uploadUrl;

    private String accessToken;

    public String getRoomUrl() {
        return roomUrl;
    }

    public void setRoomUrl(final String roomUrl) {
        this.roomUrl = roomUrl;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(final String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(final String accessToken) {
        this.accessToken = accessToken;
    }
}
