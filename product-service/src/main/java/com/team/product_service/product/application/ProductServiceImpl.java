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
import com.team.product_service.product.infrastructure.client.CompanyClient;
import com.team.product_service.product.infrastructure.client.HubClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final HubClient hubClient;
    private final CompanyClient companyClient;

    @Override
    @Transactional
    public ProductResult createProduct(ProductCreateCommand command) {
        validateDuplicateName(command.companyId(), command.name(), null);

        // companyId 존재 여부 확인
        try {
            companyClient.checkCompanyExists(command.companyId());
            log.info("[상품 생성] company id={}", command.companyId());
        } catch (FeignException.NotFound e) {
            log.warn("[상품 생성] 업체 없음 companyId={}", command.companyId(), e);
            throw new BusinessException(ProductErrorCode.COMPANY_NOT_FOUND);
        } catch (FeignException e) {
            log.error("[상품 생성] 업체 서비스 호출 실패 companyId={}, status={}", command.companyId(), e.status(), e);
            throw new BusinessException(ProductErrorCode.SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            log.error("[상품 생성] 업체 서비스 연결 실패 companyId={}", command.companyId(), e);
            throw new BusinessException(ProductErrorCode.SERVICE_UNAVAILABLE);
        }

        // hubId 존재 여부 확인 (허브 서비스 연결 해결 후 주석 해제)
        try {
            hubClient.checkHubExists(command.hubId());
            log.info("[상품 생성] hub id={}", command.hubId());
        } catch (FeignException.NotFound e) {
            log.warn("[상품 생성] 허브 없음 hubId={}", command.hubId(), e);
            throw new BusinessException(ProductErrorCode.HUB_NOT_FOUND);
        } catch (FeignException e) {
            log.error("[상품 생성] 허브 서비스 호출 실패 hubId={}, status={}", command.hubId(), e.status(), e);
            throw new BusinessException(ProductErrorCode.SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            log.error("[상품 생성] 허브 서비스 연결 실패 hubId={}", command.hubId(), e);
            throw new BusinessException(ProductErrorCode.SERVICE_UNAVAILABLE);
        }

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
