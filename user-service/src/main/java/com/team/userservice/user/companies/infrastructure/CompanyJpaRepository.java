package com.team.userservice.user.companies.infrastructure;

import com.team.userservice.user.core.CompanyUser;
import com.team.userservice.user.core.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyJpaRepository extends JpaRepository<CompanyUser, Long> {

    Optional<CompanyUser> findByUser(User user);

    List<CompanyUser> findAllByUserIn(List<User> users);
}
