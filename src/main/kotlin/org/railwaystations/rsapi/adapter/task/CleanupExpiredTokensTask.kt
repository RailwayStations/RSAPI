package org.railwaystations.rsapi.adapter.task

import org.railwaystations.rsapi.adapter.db.OAuth2AuthorizationDao
import org.railwaystations.rsapi.utils.Logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class CleanupExpiredTokensTask(private val oAuth2AuthorizationDao: OAuth2AuthorizationDao) {

    private val log by Logger()

    @Scheduled(fixedDelay = 3600000)
    fun cleanupExpiredTokens() {
        val numberOfDeletedRows = oAuth2AuthorizationDao.deleteExpiredTokens(Instant.now())
        log.info("Deleted {} expired tokens", numberOfDeletedRows)
    }
}
