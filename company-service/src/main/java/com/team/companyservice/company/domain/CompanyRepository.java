package com.team.companyservice.company.domain;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends JpaRepository<Company, UUID>, CompanyRepositoryCustom {

    Optional<Company> findByIdAndDeletedAtIsNull(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select c
        from Company c
        where c.id = :companyId
          and c.deletedAt is null
    """)
    Optional<Company> findByIdAndDeletedAtIsNullForUpdate(@Param("companyId") UUID companyId);

    boolean existsByHubIdAndNameAndDeletedAtIsNull(UUID hubId, String name);

    boolean existsByHubIdAndNameAndDeletedAtIsNullAndIdNot(UUID hubId, String name, UUID id);
}
