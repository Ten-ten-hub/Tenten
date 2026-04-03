package com.team.userservice.user.company.infrastructure;

import com.team.userservice.user.core.CompanyUser;
import com.team.userservice.user.core.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyJpaRepository extends JpaRepository<CompanyUser, UUID> {

    Optional<CompanyUser> findByUser(User user);
}
