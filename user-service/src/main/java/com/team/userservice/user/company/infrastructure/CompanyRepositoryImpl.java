package com.team.userservice.user.company.infrastructure;

import com.team.userservice.global.domain.error.UserErrorCode;
import com.team.userservice.global.exception.UserException;
import com.team.userservice.user.company.domain.CompanyRepository;
import com.team.userservice.user.core.CompanyUser;
import com.team.userservice.user.core.User;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CompanyRepositoryImpl implements CompanyRepository {
    private final CompanyJpaRepository companyJpaRepository;

    @Override
    public void update(User user, UUID affiliationId) {
        companyJpaRepository.findByUser(user)
            .ifPresentOrElse(
                companyUser -> companyUser.updateCompanyId(affiliationId),
                () -> companyJpaRepository.save(CompanyUser.create(user, affiliationId))
            );
    }

    @Override
    public CompanyUser findByUser(User user) {
        return companyJpaRepository.findByUser(user).orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    }
}
