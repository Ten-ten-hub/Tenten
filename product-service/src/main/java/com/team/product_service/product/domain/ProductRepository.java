package com.team.product_service.product.domain;

import com.team.product_service.product.application.dto.ProductGetQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {
    Product save(Product product);

    Optional<Product> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsDuplicateName(UUID companyId, String name, UUID excludeId);

    Page<Product> search(ProductGetQuery query, Pageable pageable);
}
