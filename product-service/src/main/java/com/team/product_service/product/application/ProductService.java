package com.team.product_service.product.application;

import com.team.product_service.product.application.dto.ProductCreateCommand;
import com.team.product_service.product.application.dto.ProductGetQuery;
import com.team.product_service.product.application.dto.ProductResult;
import com.team.product_service.product.application.dto.ProductUpdateCommand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProductService {
    ProductResult createProduct(ProductCreateCommand command);

    ProductResult getProduct(UUID productId);

    Page<ProductResult> getProducts(ProductGetQuery query, Pageable pageable);

    ProductResult updateProduct(ProductUpdateCommand command);

    void deleteProduct(UUID productId, UUID deletedBy);
}
