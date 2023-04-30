package org.railwaystations.rsapi.adapter.out.db;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.time.Instant;

public interface OAuth2AuthorizationDao {

    @SqlUpdate("""
            DELETE FROM oauth2_authorization
                WHERE (authorization_code_value IS NULL OR authorization_code_expires_at < :now)
                    AND
                      (access_token_value IS NULL OR access_token_expires_at < :now)
                    AND
                      (refresh_token_value IS NULL OR refresh_token_expires_at < :now)
            """)
    int deleteExpiredTokens(@Bind("now") Instant now);

    @SqlUpdate("""
            DELETE FROM oauth2_authorization
                WHERE principal_name = :principalName
            """)
    void deleteAllByUser(@Bind("principalName") String principalName);

}
