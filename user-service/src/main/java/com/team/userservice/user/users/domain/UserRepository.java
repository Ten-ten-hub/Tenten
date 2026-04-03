package com.team.userservice.user.users.domain;

import com.team.userservice.user.core.CompanyUser;
import com.team.userservice.user.core.HubUser;
import com.team.userservice.user.core.User;
import com.team.userservice.user.core.enums.AffiliatedStatus;
import com.team.userservice.user.core.enums.Role;
import com.team.userservice.user.core.enums.SignupStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepository {

    User save(User user);

    //void register(UUID userId, Role giveRole);

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    User findByLoginId(String loginId);

    void flush();

    void updateUserRole(User user, Role role);

    Optional<User> findById(UUID userId);

    void updateHubUser(User user, UUID affiliationId);

    void updateCompanyUser(User user, UUID affiliationId);

    HubUser findHubUser(User user);

    CompanyUser findCompanyUser(User user);

    Page<User> findAll(Pageable pageable);

    Page<User> findAll(List<Role> roles, AffiliatedStatus affiliatedStatus, Pageable pageable);

    List<User> findAll();

    List<User> findAll(List<Role> roles, AffiliatedStatus affiliatedStatus);

    Page<User> findAllBySignupStatus(SignupStatus signupStatus, Pageable pageable);
}
