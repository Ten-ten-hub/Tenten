package com.team.product_service.product.presentation;

import com.team.common.exception.BusinessException;
import com.team.product_service.product.domain.Product;
import com.team.product_service.product.domain.ProductRepository;
import com.team.product_service.global.exception.ProductErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;

    @GetMapping("/{productId}")
    public ResponseEntity<Product> commonErrorTest(@PathVariable UUID productId) {
        return productRepository.findById(productId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }
}
