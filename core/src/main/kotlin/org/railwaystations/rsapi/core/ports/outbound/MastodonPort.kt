package org.railwaystations.rsapi.core.ports.outbound

import org.springframework.scheduling.annotation.Async

interface MastodonPort {
    @Async
    fun tootNewPhoto(status: String)
}