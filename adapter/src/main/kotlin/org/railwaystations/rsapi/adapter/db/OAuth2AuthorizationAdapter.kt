package org.railwaystations.rsapi.adapter.db

import org.jooq.DSLContext
import org.railwaystations.rsapi.adapter.db.jooq.tables.references.Oauth2AuthorizationTable
import org.railwaystations.rsapi.core.ports.outbound.OAuth2AuthorizationPort
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class OAuth2AuthorizationAdapter(private val dsl: DSLContext) : OAuth2AuthorizationPort {

    override fun deleteExpiredTokens(now: Instant): Int {
        return dsl.deleteFrom(Oauth2AuthorizationTable)
            .where(
                Oauth2AuthorizationTable.authorizationCodeValue.isNull.or(
                    Oauth2AuthorizationTable.authorizationCodeExpiresAt.lt(now)
                )
            )
            .and(
                Oauth2AuthorizationTable.accessTokenValue.isNull.or(
                    (Oauth2AuthorizationTable.accessTokenExpiresAt.lt(now))
                )
            )
            .and(
                Oauth2AuthorizationTable.refreshTokenValue.isNull.or(
                    Oauth2AuthorizationTable.refreshTokenExpiresAt.lt(now)
                )
            )
            .execute()
    }

    override fun deleteAllByUser(principalName: String) {
        dsl.deleteFrom(Oauth2AuthorizationTable)
            .where(Oauth2AuthorizationTable.principalName.eq(principalName))
            .execute()
    }
}
