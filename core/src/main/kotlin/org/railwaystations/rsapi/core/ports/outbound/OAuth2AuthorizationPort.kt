package org.railwaystations.rsapi.core.ports.outbound

import java.time.Instant

interface OAuth2AuthorizationPort {
    fun deleteExpiredTokens(now: Instant?): Int
    fun deleteAllByUser(principalName: String?)
}