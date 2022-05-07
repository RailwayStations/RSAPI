package org.railwaystations.rsapi.adapter.out.monitoring;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "matrix")
@Data
@NoArgsConstructor
public class MatrixMonitorConfig {

    private String roomUrl;

    private String uploadUrl;

    private String accessToken;

}
