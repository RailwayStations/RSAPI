package org.railwaystations.rsapi.adapter.in.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.railwaystations.rsapi.adapter.out.db.OAuth2AuthorizationDao;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class CleanupExpiredTokensTask {

    private final OAuth2AuthorizationDao oAuth2AuthorizationDao;

    @Scheduled(fixedDelay = 3_600_000)
    public void cleanupExpiredTokens() {
        int numberOfDeletedRows = oAuth2AuthorizationDao.deleteExpiredTokens(Instant.now());
        log.info("Deleted {} expired tokens", numberOfDeletedRows);
    }

}
