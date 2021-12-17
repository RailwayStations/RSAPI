package org.railwaystations.rsapi.mastodon;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "mastodon-bot")
public class MastodonBotConfig {

    private String token;

    private String stationUrl;

    private String instanceUrl;


    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    public String getStationUrl() {
        return stationUrl;
    }

    public void setStationUrl(final String stationUrl) {
        this.stationUrl = stationUrl;
    }

    public String getInstanceUrl() {
        return instanceUrl;
    }

    public void setInstanceUrl(final String instanceUrl) {
        this.instanceUrl = instanceUrl;
    }
}
