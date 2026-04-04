package com.team.userservice.user.users.infrastructure;

import com.team.userservice.global.domain.error.UserErrorCode;
import com.team.userservice.global.exception.UserException;
import com.team.userservice.user.companies.domain.CompanyRepository;
import com.team.userservice.user.core.CompanyUser;
import com.team.userservice.user.core.HubUser;
import com.team.userservice.user.core.User;
import com.team.userservice.user.core.enums.AffiliatedStatus;
import com.team.userservice.user.core.enums.Role;
import com.team.userservice.user.core.enums.SignupStatus;
import org.springframework.data.jpa.domain.Specification;
import com.team.userservice.user.hubs.domain.HubRepository;
import com.team.userservice.user.users.domain.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {


    private final UserJpaRepository userJpaRepository;
    private final HubRepository hubRepository;
    private final CompanyRepository companyRepository;

    @Override
    public User save(User user) {
        return userJpaRepository.save(user);
    }

//    @Override
//    public void register(UUID userId, Role giveRole) {
//        User user = userJpaRepository.findById(userId).orElseThrow(
//            () -> new UserException(UserErrorCode.USER_NOT_FOUND));
//        user.register(giveRole);
//        if()
//    }

    @Override
    public boolean existsByLoginId(String loginId) {
        return userJpaRepository.existsByLoginId(loginId);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public User findByLoginId(String loginId) {
        return userJpaRepository.findByLoginId(loginId)
            .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    }

    @Override
    public void flush() {
        userJpaRepository.flush();
    }

    @Override
    public void updateUserRole(User user, Role role) {
        user.updateRole(role);
    }

    @Override
    public Optional<User> findById(UUID userId) {
        return userJpaRepository.findById(userId);
    }

    @Override
    public void updateHubUser(User user, UUID affiliationId) {
        hubRepository.save(user, affiliationId);
    }

    @Override
    public void updateCompanyUser(User user, UUID affiliationId) {
        companyRepository.save(user, affiliationId);
    }

    @Override
    public HubUser findHubUser(User user) {
        return hubRepository.findByUser(user);
    }

    @Override
    public CompanyUser findCompanyUser(User user) {
        return companyRepository.findByUser(user);
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        return userJpaRepository.findAll(pageable);
    }

    @Override
    public Page<User> findAll(List<Role> roles, AffiliatedStatus affiliatedStatus, Pageable pageable) {
        Specification<User> spec = UserSpecification.hasRoles(roles)
            .and(UserSpecification.hasAffiliatedStatus(affiliatedStatus));
        return userJpaRepository.findAll(spec, pageable);
    }

    @Override
    public List<User> findAll() {
        return userJpaRepository.findAll();
    }

    @Override
    public List<User> findAll(List<Role> roles, AffiliatedStatus affiliatedStatus) {
        Specification<User> spec = UserSpecification.hasRoles(roles)
            .and(UserSpecification.hasAffiliatedStatus(affiliatedStatus));
        return userJpaRepository.findAll(spec);
    }

    @Override
    public Page<User> findAllBySignupStatus(SignupStatus signupStatus, Pageable pageable) {
        return userJpaRepository.findAllBySignupStatus(signupStatus, pageable);
    }
}
