package org.railwaystations.rsapi.core.ports

import org.springframework.scheduling.annotation.Async

interface MastodonBot {
    @Async
    fun tootNewPhoto(status: String)
}
