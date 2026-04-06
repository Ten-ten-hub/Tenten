package com.team.product_service.stock.application;

import com.team.common.exception.BusinessException;
import com.team.product_service.global.exception.ProductErrorCode;
import com.team.product_service.stock.application.dto.*;
import com.team.product_service.stock.domain.*;
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
class StockServiceImplTest {

    @InjectMocks
    private StockServiceImpl stockService;

    @Mock
    private StockRepository stockRepository;

    private final UUID productId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    private Stock createStock(int quantity) {
        return Stock.builder()
            .productId(productId)
            .hubId(UUID.randomUUID())
            .quantity(quantity)
            .status(quantity == 0 ? StockStatus.SOLD_OUT
                : quantity <= 10 ? StockStatus.SHORTAGE
                : StockStatus.AVAILABLE)
            .build();
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // 재고 조회
    // ══════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("재고 조회")
    class GetStock {

        @Test
        @DisplayName("정상적으로 재고를 조회한다")
        void success() {
            Stock stock = createStock(100);
            given(stockRepository.findByProductIdAndDeletedAtIsNull(productId))
                .willReturn(Optional.of(stock));

            StockResult result = stockService.getStock(productId);

            assertThat(result.productId()).isEqualTo(productId);
            assertThat(result.quantity()).isEqualTo(100);
            assertThat(result.status()).isEqualTo(StockStatus.AVAILABLE);
        }

        @Test
        @DisplayName("재고가 존재하지 않으면 예외가 발생한다")
        void notFound() {
            given(stockRepository.findByProductIdAndDeletedAtIsNull(productId))
                .willReturn(Optional.empty());

            assertThatThrownBy(() -> stockService.getStock(productId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(ProductErrorCode.STOCK_NOT_FOUND));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // 재고 조정
    // ══════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("재고 조정")
    class AdjustStock {

        @Test
        @DisplayName("정상적으로 재고를 조정한다")
        void success() {
            Stock stock = createStock(50);
            given(stockRepository.findByProductIdAndDeletedAtIsNull(productId))
                .willReturn(Optional.of(stock));
            given(stockRepository.saveHistory(any())).willAnswer(inv -> inv.getArgument(0));

            StockAdjustCommand command = new StockAdjustCommand(productId, 100);
            StockResult result = stockService.adjustStock(command);

            assertThat(result.quantity()).isEqualTo(100);
            assertThat(result.status()).isEqualTo(StockStatus.AVAILABLE);
            verify(stockRepository).saveHistory(any(StockHistory.class));
        }

        @Test
        @DisplayName("재고를 0으로 조정하면 SOLD_OUT 상태가 된다")
        void adjustToZero_soldOut() {
            Stock stock = createStock(50);
            given(stockRepository.findByProductIdAndDeletedAtIsNull(productId))
                .willReturn(Optional.of(stock));
            given(stockRepository.saveHistory(any())).willAnswer(inv -> inv.getArgument(0));

            StockAdjustCommand command = new StockAdjustCommand(productId, 0);
            StockResult result = stockService.adjustStock(command);

            assertThat(result.quantity()).isEqualTo(0);
            assertThat(result.status()).isEqualTo(StockStatus.SOLD_OUT);
        }

        @Test
        @DisplayName("재고를 10 이하로 조정하면 SHORTAGE 상태가 된다")
        void adjustToShortage() {
            Stock stock = createStock(50);
            given(stockRepository.findByProductIdAndDeletedAtIsNull(productId))
                .willReturn(Optional.of(stock));
            given(stockRepository.saveHistory(any())).willAnswer(inv -> inv.getArgument(0));

            StockAdjustCommand command = new StockAdjustCommand(productId, 5);
            StockResult result = stockService.adjustStock(command);

            assertThat(result.quantity()).isEqualTo(5);
            assertThat(result.status()).isEqualTo(StockStatus.SHORTAGE);
        }

        @Test
        @DisplayName("재고가 존재하지 않으면 예외가 발생한다")
        void notFound() {
            given(stockRepository.findByProductIdAndDeletedAtIsNull(productId))
                .willReturn(Optional.empty());

            StockAdjustCommand command = new StockAdjustCommand(productId, 100);

            assertThatThrownBy(() -> stockService.adjustStock(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(ProductErrorCode.STOCK_NOT_FOUND));

            verify(stockRepository, never()).saveHistory(any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // 재고 차감
    // ══════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("재고 차감")
    class DeductStock {

        @Test
        @DisplayName("정상적으로 재고를 차감한다")
        void success() {
            Stock stock = createStock(100);
            given(stockRepository.findByProductIdAndDeletedAtIsNull(productId))
                .willReturn(Optional.of(stock));
            given(stockRepository.saveHistory(any())).willAnswer(inv -> inv.getArgument(0));

            StockDeductCommand command = new StockDeductCommand(productId, 30, orderId);
            stockService.deductStock(command);

            assertThat(stock.getQuantity()).isEqualTo(70);
            verify(stockRepository).saveHistory(any(StockHistory.class));
        }

        @Test
        @DisplayName("재고보다 많은 수량을 차감하면 예외가 발생한다")
        void notEnough() {
            Stock stock = createStock(10);
            given(stockRepository.findByProductIdAndDeletedAtIsNull(productId))
                .willReturn(Optional.of(stock));

            StockDeductCommand command = new StockDeductCommand(productId, 50, orderId);

            assertThatThrownBy(() -> stockService.deductStock(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(ProductErrorCode.STOCK_NOT_ENOUGH));

            verify(stockRepository, never()).saveHistory(any());
        }

        @Test
        @DisplayName("재고가 존재하지 않으면 예외가 발생한다")
        void notFound() {
            given(stockRepository.findByProductIdAndDeletedAtIsNull(productId))
                .willReturn(Optional.empty());

            StockDeductCommand command = new StockDeductCommand(productId, 10, orderId);

            assertThatThrownBy(() -> stockService.deductStock(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(ProductErrorCode.STOCK_NOT_FOUND));

            verify(stockRepository, never()).saveHistory(any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // 재고 복원
    // ══════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("재고 복원")
    class RestoreStock {

        @Test
        @DisplayName("정상적으로 재고를 복원한다")
        void success() {
            Stock stock = createStock(50);
            given(stockRepository.findByProductIdAndDeletedAtIsNull(productId))
                .willReturn(Optional.of(stock));
            given(stockRepository.saveHistory(any())).willAnswer(inv -> inv.getArgument(0));

            StockRestoreCommand command = new StockRestoreCommand(productId, 30, orderId);
            stockService.restoreStock(command);

            assertThat(stock.getQuantity()).isEqualTo(80);
            verify(stockRepository).saveHistory(any(StockHistory.class));
        }

        @Test
        @DisplayName("재고가 존재하지 않으면 예외가 발생한다")
        void notFound() {
            given(stockRepository.findByProductIdAndDeletedAtIsNull(productId))
                .willReturn(Optional.empty());

            StockRestoreCommand command = new StockRestoreCommand(productId, 30, orderId);

            assertThatThrownBy(() -> stockService.restoreStock(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(ProductErrorCode.STOCK_NOT_FOUND));

            verify(stockRepository, never()).saveHistory(any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // 재고 이력 조회
    // ══════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("재고 이력 조회")
    class GetStockHistories {

        @Test
        @DisplayName("stockId로 이력을 조회한다")
        void byStockId() {
            UUID stockId = UUID.randomUUID();
            StockHistoryGetQuery query = new StockHistoryGetQuery(stockId, null, null, null);
            PageRequest pageable = PageRequest.of(0, 10);

            StockHistory history = StockHistory.of(stockId, StockHistoryType.ADJUSTMENT, 100, 100, null);
            Page<StockHistory> page = new PageImpl<>(List.of(history), pageable, 1);
            given(stockRepository.searchHistory(stockId, null, null, pageable)).willReturn(page);

            Page<StockHistoryResult> result = stockService.getStockHistories(query, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).type()).isEqualTo(StockHistoryType.ADJUSTMENT);
        }

        @Test
        @DisplayName("productId로 이력을 조회한다")
        void byProductId() {
            UUID stockId = UUID.randomUUID();
            Stock stock = createStock(100);
            StockHistoryGetQuery query = new StockHistoryGetQuery(null, productId, null, null);
            PageRequest pageable = PageRequest.of(0, 10);

            given(stockRepository.findByProductIdAndDeletedAtIsNull(productId))
                .willReturn(Optional.of(stock));

            StockHistory history = StockHistory.of(stockId, StockHistoryType.OUTBOUND, -10, 90, orderId);
            Page<StockHistory> page = new PageImpl<>(List.of(history), pageable, 1);
            given(stockRepository.searchHistory(any(), any(), any(), any())).willReturn(page);

            Page<StockHistoryResult> result = stockService.getStockHistories(query, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).type()).isEqualTo(StockHistoryType.OUTBOUND);
        }

        @Test
        @DisplayName("productId로 조회 시 재고가 없으면 빈 페이지를 반환한다")
        void byProductId_stockNotFound_returnsEmpty() {
            StockHistoryGetQuery query = new StockHistoryGetQuery(null, productId, null, null);
            PageRequest pageable = PageRequest.of(0, 10);

            given(stockRepository.findByProductIdAndDeletedAtIsNull(productId))
                .willReturn(Optional.empty());

            Page<StockHistoryResult> result = stockService.getStockHistories(query, pageable);

            assertThat(result.isEmpty()).isTrue();
            verify(stockRepository, never()).searchHistory(any(), any(), any(), any());
        }
    }
}
