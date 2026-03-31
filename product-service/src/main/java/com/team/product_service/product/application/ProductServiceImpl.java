package com.team.product_service.product.application;

import com.team.product_service.product.application.dto.ProductCreateCommand;
import com.team.product_service.product.application.dto.ProductGetQuery;
import com.team.product_service.product.application.dto.ProductResult;
import com.team.product_service.product.application.dto.ProductUpdateCommand;
import com.team.product_service.product.domain.Product;
import com.team.product_service.product.domain.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public ProductResult createProduct(ProductCreateCommand command) {
        validateDuplicateName(command.companyId(), command.name(), null);

        // TODO: companyId 존재 여부 확인 (Feign Client 연동 시 추가)
        // TODO: hubId 존재 여부 확인 (Feign Client 연동 시 추가)

        Product product = Product.create(
            command.name(),
            command.companyId(),
            command.hubId(),
            command.unitPrice(),
            command.description()
        );
        return ProductResult.from(productRepository.save(product));
    }


    @Override
    public ProductResult getProduct(UUID productId) {

        Product product = findActiveProductById(productId);
        return ProductResult.from(product);
    }

    @Override
    public Page<ProductResult> getProducts(ProductGetQuery query, Pageable pageable) {
        return productRepository.search(query, pageable)
            .map(ProductResult::from);
    }

    @Override
    @Transactional
    public ProductResult updateProduct(ProductUpdateCommand command) {
        Product product = findActiveProductById(command.productId());

        if (command.name() != null) {
            validateDuplicateName(product.getCompanyId(), command.name(), command.productId());
        }

        product.update(command.name(), command.unitPrice(), command.description(), command.status());
        return ProductResult.from(product);

    }

    @Override
    @Transactional
    public void deleteProduct(UUID productId, UUID deletedBy) {
        Product product = findActiveProductById(productId);
        product.softDelete(deletedBy);
    }

    private Product findActiveProductById(UUID productId) {
        // common 모듈 pull 받은 뒤 수정
        return productRepository.findByIdAndDeletedAtIsNull(productId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));
    }

    private void validateDuplicateName(UUID companyId, String name, UUID excludedId) {

        if (productRepository.existsDuplicateName(companyId, name, excludedId)) {
            // 공통 모듈 pull 받은 뒤 에러 코드 수정
            throw new IllegalArgumentException("같은 업체 내 동일한 상품명이 이미 존재합니다. name=" + name);
        }
    }
}
