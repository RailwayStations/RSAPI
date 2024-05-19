package org.railwaystations.rsapi.adapter.db

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.railwaystations.rsapi.core.ports.OAuth2AuthorizationPort
import java.time.Instant

interface OAuth2AuthorizationDao : OAuth2AuthorizationPort {
    @SqlUpdate(
        """
            DELETE FROM oauth2_authorization
                WHERE (authorization_code_value IS NULL OR authorization_code_expires_at < :now)
                    AND
                      (access_token_value IS NULL OR access_token_expires_at < :now)
                    AND
                      (refresh_token_value IS NULL OR refresh_token_expires_at < :now)
            
            """
    )
    override fun deleteExpiredTokens(@Bind("now") now: Instant?): Int

    @SqlUpdate(
        """
            DELETE FROM oauth2_authorization
                WHERE principal_name = :principalName
            
            """
    )
    override fun deleteAllByUser(@Bind("principalName") principalName: String?)
}
