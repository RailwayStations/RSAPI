package org.railwaystations.rsapi.adapter.task;

import org.railwaystations.rsapi.services.NotifyUsersService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("PMD.BeanMembersShouldSerialize")
public class NotifyUsersTask {

    private final NotifyUsersService notifyUsersService;

    public NotifyUsersTask(final NotifyUsersService notifyUsersService) {
        super();
        this.notifyUsersService = notifyUsersService;
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void notifyUsers() {
        notifyUsersService.notifyUsers();
    }

}
