package com.team.userservice.user.companies.infrastructure;

import com.team.userservice.global.domain.error.UserErrorCode;
import com.team.userservice.global.exception.UserException;
import com.team.userservice.user.companies.domain.CompanyRepository;
import com.team.userservice.user.core.CompanyUser;
import com.team.userservice.user.core.User;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CompanyRepositoryImpl implements CompanyRepository {

    private final CompanyJpaRepository companyJpaRepository;

    @Override
    public void save(User user, UUID affiliationId) {
        companyJpaRepository.save(CompanyUser.create(user, affiliationId));
    }

    @Override
    public CompanyUser findByUser(User user) {
        return companyJpaRepository.findByUser(user)
            .orElseThrow(() -> new UserException(UserErrorCode.COM_USER_NOT_FOUND));
    }

    @Override
    public List<CompanyUser> findAllByUsers(List<User> users) {
        return companyJpaRepository.findAllByUserIn(users);
    }
}
