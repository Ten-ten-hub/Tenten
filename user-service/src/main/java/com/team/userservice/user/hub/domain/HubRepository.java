package com.team.userservice.user.hub.domain;

import com.team.userservice.user.core.HubUser;
import com.team.userservice.user.core.User;
import java.util.UUID;

public interface HubRepository {
    void update(User user, UUID affiliationId);

    HubUser findByUser(User user);
}
