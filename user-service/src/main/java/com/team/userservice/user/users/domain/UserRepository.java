package com.team.userservice.user.users.domain;

import com.team.userservice.user.core.User;
import com.team.userservice.user.core.enums.Role;
import java.util.UUID;

public interface UserRepository {

    User save(User user);

    void register(UUID userId, Role giveRole);

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    User findByLoginId(String loginId);
}
