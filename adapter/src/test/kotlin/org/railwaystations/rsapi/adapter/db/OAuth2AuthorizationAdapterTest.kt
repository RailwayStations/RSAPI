package org.railwaystations.rsapi.adapter.db

import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.Test
import org.railwaystations.rsapi.adapter.db.jooq.tables.records.Oauth2AuthorizationRecord
import org.railwaystations.rsapi.adapter.db.jooq.tables.references.Oauth2AuthorizationTable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import java.time.Instant
import java.time.temporal.ChronoUnit

@JooqTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(
    JooqCustomizerConfiguration::class,
    OAuth2AuthorizationAdapter::class,
)
class OAuth2AuthorizationAdapterTest : AbstractPostgreSqlTest() {

    @Autowired
    private lateinit var dsl: DSLContext

    @Autowired
    private lateinit var sut: OAuth2AuthorizationAdapter

    @Test
    fun deleteExpiredTokens() {
        val past = Instant.now().minus(1, ChronoUnit.MINUTES)
        val future = Instant.now().plus(1, ChronoUnit.MINUTES)
        insertRecord(id = "1")
        insertRecord(id = "2", authorizationCodeValue = "authorizationCodeValue", authorizationCodeExpiresAt = past)
        insertRecord(id = "3", authorizationCodeValue = "authorizationCodeValue", authorizationCodeExpiresAt = future)
        insertRecord(id = "4", accessTokenValue = "accessTokenValue", accessTokenExpiresAt = past)
        insertRecord(id = "5", accessTokenValue = "accessTokenValue", accessTokenExpiresAt = future)
        insertRecord(id = "6", refreshTokenValue = "refreshTokenValue", refreshTokenExpiresAt = past)
        insertRecord(id = "7", refreshTokenValue = "refreshTokenValue", refreshTokenExpiresAt = future)
        insertRecord(
            id = "8",
            authorizationCodeValue = "authorizationCodeValue",
            authorizationCodeExpiresAt = past,
            accessTokenValue = "accessTokenValue",
            accessTokenExpiresAt = past,
            refreshTokenValue = "refreshTokenValue",
            refreshTokenExpiresAt = future,
        )

        val deleteCount = sut.deleteExpiredTokens(Instant.now())

        assertThat(deleteCount).isEqualTo(4)
        val remainingIds = dsl.selectFrom(Oauth2AuthorizationTable).fetch().map { it.id }
        assertThat(remainingIds).containsExactlyInAnyOrder("3", "5", "7", "8")
    }

    private fun insertRecord(
        id: String,
        authorizationCodeValue: String? = null,
        authorizationCodeExpiresAt: Instant? = null,
        accessTokenValue: String? = null,
        accessTokenExpiresAt: Instant? = null,
        refreshTokenValue: String? = null,
        refreshTokenExpiresAt: Instant? = null,
        principalName: String = "principalName",
    ) {
        dsl.executeInsert(
            Oauth2AuthorizationRecord(
                id = id,
                registeredClientId = "registeredClientId",
                principalName = principalName,
                authorizationGrantType = "authorizationGrantType",
                authorizedScopes = null,
                attributes = null,
                state = null,
                authorizationCodeValue = authorizationCodeValue,
                authorizationCodeIssuedAt = null,
                authorizationCodeExpiresAt = authorizationCodeExpiresAt,
                authorizationCodeMetadata = null,
                accessTokenValue = accessTokenValue,
                accessTokenIssuedAt = null,
                accessTokenExpiresAt = accessTokenExpiresAt,
                accessTokenMetadata = null,
                accessTokenType = null,
                accessTokenScopes = null,
                oidcIdTokenValue = null,
                oidcIdTokenIssuedAt = null,
                oidcIdTokenExpiresAt = null,
                oidcIdTokenMetadata = null,
                refreshTokenValue = refreshTokenValue,
                refreshTokenIssuedAt = null,
                refreshTokenExpiresAt = refreshTokenExpiresAt,
                refreshTokenMetadata = null,
                userCodeValue = null,
                userCodeIssuedAt = null,
                userCodeExpiresAt = null,
                userCodeMetadata = null,
                deviceCodeValue = null,
                deviceCodeIssuedAt = null,
                deviceCodeExpiresAt = null,
                deviceCodeMetadata = null,
            )
        )
    }

    @Test
    fun deleteAllByUser() {
        insertRecord(id = "1", principalName = "Name 1")
        insertRecord(id = "2", principalName = "Name 2")

        sut.deleteAllByUser("Name 1")

        val remainingIds = dsl.selectFrom(Oauth2AuthorizationTable).fetch().map { it.id }
        assertThat(remainingIds).containsExactlyInAnyOrder("2")
    }

}