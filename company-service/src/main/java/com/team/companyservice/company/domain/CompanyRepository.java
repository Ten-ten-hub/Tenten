package com.team.companyservice.company.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, UUID>, CompanyRepositoryCustom {

    Optional<Company> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByHubIdAndNameAndDeletedAtIsNull(UUID hubId, String name);

    boolean existsByHubIdAndNameAndDeletedAtIsNullAndIdNot(UUID hubId, String name, UUID id);
}
