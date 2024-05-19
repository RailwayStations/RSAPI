package org.railwaystations.rsapi.adapter.task

import org.railwaystations.rsapi.core.ports.inbound.NotifyUsersUseCase
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class NotifyUsersTask(private val notifyUsersUseCase: NotifyUsersUseCase) {

    @Scheduled(cron = "0 0 1 * * *")
    fun notifyUsers() {
        notifyUsersUseCase.notifyUsers()
    }
}
