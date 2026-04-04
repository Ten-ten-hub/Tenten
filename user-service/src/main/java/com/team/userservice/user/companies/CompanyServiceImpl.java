package com.team.userservice.user.companies;

import com.team.userservice.user.companies.application.CompanyService;
import com.team.userservice.user.companies.domain.CompanyRepository;
import com.team.userservice.user.core.CompanyUser;
import com.team.userservice.user.core.User;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {
    private final CompanyRepository companyRepository;

    @Override
    public void save(User user, UUID affiliationId) {
        companyRepository.save(user, affiliationId);
    }

    @Override
    public CompanyUser findByUser(User user) {
        return companyRepository.findByUser(user);
    }
}
