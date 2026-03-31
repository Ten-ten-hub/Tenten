package com.team.product_service.product.infrastructure;

import com.team.product_service.product.application.dto.ProductGetQuery;
import com.team.product_service.product.domain.Product;
import com.team.product_service.product.domain.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository jpaRepository;

    @Override
    public Product save(Product product) {
        return jpaRepository.save(product);
    }

    @Override
    public Optional<Product> findByIdAndDeletedAtIsNull(UUID id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public boolean existsDuplicateName(UUID companyId, String name, UUID excludeId) {
        return excludeId == null
                ? jpaRepository.existsByCompanyIdAndNameAndDeletedAtIsNull(companyId, name)
                : jpaRepository.existsByCompanyIdAndNameAndIdNotAndDeletedAtIsNull(companyId, name, excludeId);
    }

    @Override
    public Page<Product> search(ProductGetQuery query, Pageable pageable) {
        return jpaRepository.search(query.name(), query.companyId(), query.hubId(), query.status(), pageable);
    }
}
