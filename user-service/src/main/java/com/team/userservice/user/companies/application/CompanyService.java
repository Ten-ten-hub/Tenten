package com.team.userservice.user.companies.application;

import com.team.userservice.user.core.CompanyUser;
import com.team.userservice.user.core.User;
import java.util.UUID;

public interface CompanyService {
    void save(User user, UUID affiliationId);

    CompanyUser findByUser(User user);
}
