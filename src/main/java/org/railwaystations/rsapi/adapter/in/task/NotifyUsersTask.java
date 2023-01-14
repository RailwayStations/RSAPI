package org.railwaystations.rsapi.adapter.in.task;

import lombok.RequiredArgsConstructor;
import org.railwaystations.rsapi.core.services.NotifyUsersService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotifyUsersTask {

    private final NotifyUsersService notifyUsersService;

    @Scheduled(cron = "0 0 1 * * *")
    public void notifyUsers() {
        notifyUsersService.notifyUsers();
    }

}
