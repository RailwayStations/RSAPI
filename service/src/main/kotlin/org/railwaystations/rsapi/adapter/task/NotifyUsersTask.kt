package org.railwaystations.rsapi.adapter.task

import org.railwaystations.rsapi.core.services.NotifyUsersService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class NotifyUsersTask(private val notifyUsersService: NotifyUsersService) {

    @Scheduled(cron = "0 0 1 * * *")
    fun notifyUsers() {
        notifyUsersService.notifyUsers()
    }
}
