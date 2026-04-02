package com.team.product_service.product.application;

import com.team.common.exception.BusinessException;
import com.team.product_service.global.exception.ProductErrorCode;
import com.team.product_service.product.application.dto.ProductCreateCommand;
import com.team.product_service.product.application.dto.ProductGetQuery;
import com.team.product_service.product.application.dto.ProductResult;
import com.team.product_service.product.application.dto.ProductUpdateCommand;
import com.team.product_service.product.domain.Product;
import com.team.product_service.product.domain.ProductRepository;
import com.team.product_service.product.domain.event.ProductCreatedEvent;
import com.team.product_service.product.domain.event.ProductDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

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

        Product saved = productRepository.save(product);

        eventPublisher.publishEvent(new ProductCreatedEvent(saved.getId(), saved.getHubId()));

        return ProductResult.from(saved);
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
        
        eventPublisher.publishEvent(new ProductDeletedEvent(productId, deletedBy));
    }

    private Product findActiveProductById(UUID productId) {
        return productRepository.findByIdAndDeletedAtIsNull(productId)
            .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }

    private void validateDuplicateName(UUID companyId, String name, UUID excludedId) {

        if (productRepository.existsDuplicateName(companyId, name, excludedId)) {
            throw new BusinessException(ProductErrorCode.DUPLICATE_PRODUCT_NAME);
        }
    }
}
