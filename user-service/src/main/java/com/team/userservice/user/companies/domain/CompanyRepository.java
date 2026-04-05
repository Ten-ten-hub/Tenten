package com.team.userservice.user.companies.domain;

import com.team.userservice.user.core.CompanyUser;
import com.team.userservice.user.core.User;
import java.util.UUID;

public interface CompanyRepository {

    void save(User user, UUID affiliationId);

    CompanyUser findByUser(User user);
}
