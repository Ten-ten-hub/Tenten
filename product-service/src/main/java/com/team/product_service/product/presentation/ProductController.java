package com.team.product_service.product.presentation;

import com.team.product_service.global.PageResponse;
import com.team.product_service.product.application.ProductService;
import com.team.product_service.product.application.dto.ProductResult;
import com.team.product_service.product.presentation.dto.ProductCreateRequest;
import com.team.product_service.product.presentation.dto.ProductGetRequest;
import com.team.product_service.product.presentation.dto.ProductResponse;
import com.team.product_service.product.presentation.dto.ProductUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

    // page 공통 모듈 추가 시 변경
    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> getProducts(
        @ModelAttribute ProductGetRequest request,
        @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        Pageable validatedPageable = validatePageSize(pageable);
        Page<ProductResult> results = productService.getProducts(request.toQuery(), validatedPageable);
        return ResponseEntity.ok(PageResponse.from(results.map(ProductResponse::from)));
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

    private Pageable validatePageSize(Pageable pageable) {
        int size = pageable.getPageSize();
        if (size != 10 && size != 30 && size != 50) {
            return PageRequest.of(pageable.getPageNumber(), 10, pageable.getSort());
        }
        return pageable;
    }
}
