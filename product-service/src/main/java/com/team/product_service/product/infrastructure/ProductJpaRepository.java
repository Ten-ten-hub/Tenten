package com.team.product_service.product.infrastructure;

import com.team.product_service.product.domain.Product;
import com.team.product_service.product.domain.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductJpaRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByCompanyIdAndNameAndDeletedAtIsNull(UUID companyId, String name);

    boolean existsByCompanyIdAndNameAndIdNotAndDeletedAtIsNull(UUID companyId, String name, UUID id);

    // 검색 - 나중에 QueryDSL로 교체
    @Query("""
        SELECT p FROM Product p
        WHERE p.deletedAt IS NULL
          AND (CAST(:name AS string) IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%')))
          AND (:companyId IS NULL OR p.companyId = :companyId)
          AND (:hubId IS NULL OR p.hubId = :hubId)
          AND (CAST(:status AS string) IS NULL OR p.status = :status)
        """)
    Page<Product> search(
        @Param("name") String name,
        @Param("companyId") UUID companyId,
        @Param("hubId") UUID hubId,
        @Param("status") ProductStatus status,
        Pageable pageable
    );

}
