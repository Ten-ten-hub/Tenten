package com.team.userservice.user.users.infrastructure;

import com.team.userservice.user.core.User;
import com.team.userservice.user.core.enums.SignupStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<User, UUID> {

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    Optional<User> findByLoginId(String loginId);

    Page<User> findAllBySignupStatus(SignupStatus signupStatus, Pageable pageable);
}
