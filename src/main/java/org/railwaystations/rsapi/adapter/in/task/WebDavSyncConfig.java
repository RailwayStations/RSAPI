package org.railwaystations.rsapi.adapter.in.task;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "webdavsync")
@Data
@NoArgsConstructor
public class WebDavSyncConfig {

    private String toProcessUrl;
    private String processedUrl;
    private String user;
    private String password;

}
