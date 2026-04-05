package com.team.userservice.user.hubs.infrastructure;

import com.team.userservice.user.core.HubUser;
import com.team.userservice.user.core.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HubJpaRepository extends JpaRepository<HubUser, UUID> {

    Optional<HubUser> findByUser(User user);
}
