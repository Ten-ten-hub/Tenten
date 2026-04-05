package com.team.userservice.user.hubs.domain;

import com.team.userservice.user.core.HubUser;
import com.team.userservice.user.core.User;
import java.util.List;
import java.util.UUID;

public interface HubRepository {

    void save(User user, UUID affiliationId);

    HubUser findByUser(User user);

    List<HubUser> findAllByUsers(List<User> users);
}
