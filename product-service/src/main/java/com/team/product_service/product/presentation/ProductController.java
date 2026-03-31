package com.team.product_service.product.presentation;

import com.team.product_service.product.application.ProductService;
import com.team.product_service.product.application.dto.ProductResult;
import com.team.product_service.product.presentation.dto.ProductCreateRequest;
import com.team.product_service.product.presentation.dto.ProductResponse;
import com.team.product_service.product.presentation.dto.ProductUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/products")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
        @RequestHeader("X-User-id") UUID requestUserId,
        @RequestHeader("X-User-Role") String requestUserRole,
        @Valid @RequestBody ProductCreateRequest request
    ) {
        ProductResult result = productService.createProduct(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ProductResponse.from(result));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(
        @PathVariable UUID productId
    ) {
        ProductResult result = productService.getProduct(productId);
        return ResponseEntity.ok(ProductResponse.from(result));
    }

    @GetMapping
    public void getProducts() {
        // common 모듈 받아와서 수정
    }

    @PatchMapping("/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(
        @RequestHeader("X-User-Id") UUID requestUserId,
        @RequestHeader("X-User-Role") String requestUserRole,
        @PathVariable UUID productId,
        @Valid @RequestBody ProductUpdateRequest request
    ) {
        ProductResult result = productService.updateProduct(request.toCommand(productId));
        return ResponseEntity.ok(ProductResponse.from(result));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(
        @RequestHeader("X-User-Id") UUID requestUserId,
        @RequestHeader("X-User-Role") String requestUserRole,
        @PathVariable UUID productId
    ) {
        productService.deleteProduct(productId, requestUserId);
        return ResponseEntity.noContent().build();
    }
}
