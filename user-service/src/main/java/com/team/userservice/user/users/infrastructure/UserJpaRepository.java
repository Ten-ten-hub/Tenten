package com.team.userservice.user.users.infrastructure;

import com.team.userservice.user.core.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<User, UUID> {

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);
}
