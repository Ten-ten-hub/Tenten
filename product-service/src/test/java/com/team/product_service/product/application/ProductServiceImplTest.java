package com.team.product_service.product.application;

import com.team.common.exception.BusinessException;
import com.team.product_service.global.exception.ProductErrorCode;
import com.team.product_service.product.application.dto.ProductCreateCommand;
import com.team.product_service.product.application.dto.ProductGetQuery;
import com.team.product_service.product.application.dto.ProductResult;
import com.team.product_service.product.application.dto.ProductUpdateCommand;
import com.team.product_service.product.domain.Product;
import com.team.product_service.product.domain.ProductRepository;
import com.team.product_service.product.domain.ProductStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @InjectMocks
    private ProductServiceImpl productService;

    @Mock
    private ProductRepository productRepository;

    // -------------------------------------------------------
    // 테스트용 픽스처
    // -------------------------------------------------------

    private final UUID productId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();
    private final UUID hubId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private Product createProduct() {
        return Product.create("마른오징어", companyId, hubId, BigDecimal.valueOf(15000), "설명");
    }

    private ProductCreateCommand createCommand() {
        return new ProductCreateCommand("마른오징어", companyId, hubId, BigDecimal.valueOf(15000), "설명");
    }

    // -------------------------------------------------------
    // createProduct
    // -------------------------------------------------------

    @Nested
    @DisplayName("상품 생성")
    class CreateProduct {

        @Test
        @DisplayName("정상적으로 상품을 생성한다")
        void success() {
            // given
            ProductCreateCommand command = createCommand();
            Product product = createProduct();

            given(productRepository.existsDuplicateName(companyId, "마른오징어", null)).willReturn(false);
            given(productRepository.save(any(Product.class))).willReturn(product);

            // when
            ProductResult result = productService.createProduct(command);

            // then
            assertThat(result.name()).isEqualTo("마른오징어");
            assertThat(result.companyId()).isEqualTo(companyId);
            assertThat(result.unitPrice()).isEqualByComparingTo(BigDecimal.valueOf(15000));
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("같은 업체 내 동일한 상품명이 존재하면 예외가 발생한다")
        void duplicateName() {
            // given
            ProductCreateCommand command = createCommand();
            given(productRepository.existsDuplicateName(companyId, "마른오징어", null)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> productService.createProduct(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(ProductErrorCode.DUPLICATE_PRODUCT_NAME));

            verify(productRepository, never()).save(any());
        }
    }

    // -------------------------------------------------------
    // getProduct
    // -------------------------------------------------------

    @Nested
    @DisplayName("상품 단건 조회")
    class GetProduct {

        @Test
        @DisplayName("정상적으로 상품을 조회한다")
        void success() {
            // given
            Product product = createProduct();
            given(productRepository.findByIdAndDeletedAtIsNull(productId)).willReturn(Optional.of(product));

            // when
            ProductResult result = productService.getProduct(productId);

            // then
            assertThat(result.name()).isEqualTo("마른오징어");
            assertThat(result.companyId()).isEqualTo(companyId);
        }

        @Test
        @DisplayName("존재하지 않는 상품 조회 시 예외가 발생한다")
        void notFound() {
            // given
            given(productRepository.findByIdAndDeletedAtIsNull(productId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> productService.getProduct(productId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND));
        }
    }

    // -------------------------------------------------------
    // getProducts
    // -------------------------------------------------------

    @Nested
    @DisplayName("상품 목록 검색")
    class GetProducts {

        @Test
        @DisplayName("조건에 맞는 상품 목록을 반환한다")
        void success() {
            // given
            ProductGetQuery query = new ProductGetQuery("마른오징어", companyId, hubId, ProductStatus.ON_SALE);
            PageRequest pageable = PageRequest.of(0, 10);
            List<Product> products = List.of(createProduct(), createProduct());
            Page<Product> page = new PageImpl<>(products, pageable, 2);

            given(productRepository.search(query, pageable)).willReturn(page);

            // when
            Page<ProductResult> result = productService.getProducts(query, pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).name()).isEqualTo("마른오징어");
        }
    }

    // -------------------------------------------------------
    // updateProduct
    // -------------------------------------------------------

    @Nested
    @DisplayName("상품 수정")
    class UpdateProduct {

        @Test
        @DisplayName("정상적으로 상품을 수정한다")
        void success() {
            // given
            Product product = createProduct();
            ProductUpdateCommand command = new ProductUpdateCommand(
                productId, "황태채", BigDecimal.valueOf(20000), "새 설명", ProductStatus.ON_SALE
            );

            given(productRepository.findByIdAndDeletedAtIsNull(productId)).willReturn(Optional.of(product));
            given(productRepository.existsDuplicateName(companyId, "황태채", productId)).willReturn(false);

            // when
            ProductResult result = productService.updateProduct(command);

            // then
            assertThat(result.name()).isEqualTo("황태채");
            assertThat(result.unitPrice()).isEqualByComparingTo(BigDecimal.valueOf(20000));
        }

        @Test
        @DisplayName("이름 변경 시 중복된 이름이면 예외가 발생한다")
        void duplicateName() {
            // given
            Product product = createProduct();
            ProductUpdateCommand command = new ProductUpdateCommand(
                productId, "황태채", null, null, null
            );

            given(productRepository.findByIdAndDeletedAtIsNull(productId)).willReturn(Optional.of(product));
            given(productRepository.existsDuplicateName(companyId, "황태채", productId)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> productService.updateProduct(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(ProductErrorCode.DUPLICATE_PRODUCT_NAME));
        }

        @Test
        @DisplayName("이름을 변경하지 않으면 중복 검증을 하지 않는다")
        void noNameChange() {
            // given
            Product product = createProduct();
            ProductUpdateCommand command = new ProductUpdateCommand(
                productId, null, BigDecimal.valueOf(20000), null, null
            );

            given(productRepository.findByIdAndDeletedAtIsNull(productId)).willReturn(Optional.of(product));

            // when
            productService.updateProduct(command);

            // then
            verify(productRepository, never()).existsDuplicateName(any(), any(), any());
        }

        @Test
        @DisplayName("존재하지 않는 상품 수정 시 예외가 발생한다")
        void notFound() {
            // given
            ProductUpdateCommand command = new ProductUpdateCommand(
                productId, "황태채", null, null, null
            );
            given(productRepository.findByIdAndDeletedAtIsNull(productId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> productService.updateProduct(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND));
        }
    }

    // -------------------------------------------------------
    // deleteProduct
    // -------------------------------------------------------

    @Nested
    @DisplayName("상품 삭제")
    class DeleteProduct {

        @Test
        @DisplayName("정상적으로 상품을 삭제한다")
        void success() {
            // given
            Product product = createProduct();
            given(productRepository.findByIdAndDeletedAtIsNull(productId)).willReturn(Optional.of(product));

            // when
            productService.deleteProduct(productId, userId);

            // then
            assertThat(product.getDeletedAt()).isNotNull();
            assertThat(product.getDeletedBy()).isEqualTo(userId);
        }

        @Test
        @DisplayName("존재하지 않는 상품 삭제 시 예외가 발생한다")
        void notFound() {
            // given
            given(productRepository.findByIdAndDeletedAtIsNull(productId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> productService.deleteProduct(productId, userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND));
        }
    }
}
