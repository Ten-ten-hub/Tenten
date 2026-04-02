package com.team.deliveryservice.application.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.team.deliveryservice.domain.delivery.Delivery;
import com.team.deliveryservice.domain.delivery.DeliveryRepository;
import com.team.deliveryservice.domain.delivery.DeliveryRouteLog;
import com.team.deliveryservice.domain.delivery.DeliveryRouteLogRepository;
import com.team.deliveryservice.presentation.common.CurrentUser;
import com.team.deliveryservice.presentation.common.DeliveryErrorCode;
import com.team.deliveryservice.presentation.common.ServiceException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceImplTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private DeliveryRouteLogRepository deliveryRouteLogRepository;

    @InjectMocks
    private DeliveryServiceImpl deliveryService;

    @Test
    @DisplayName("배송 생성 성공")
    void create_delivery_success() {
        CreateDeliveryRequest request = new CreateDeliveryRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "서울시 강남구 테헤란로 123",
            "101호",
            "홍길동",
            "U12345678",
            UUID.randomUUID(),
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );

        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "MASTER_ADMIN", null, null);

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(request.orderId())).willReturn(false);
        given(deliveryRepository.save(any(Delivery.class))).willAnswer(invocation -> invocation.getArgument(0));

        DeliveryResponse response = deliveryService.createDelivery(request, currentUser);

        assertThat(response.orderId()).isEqualTo(request.orderId());
        assertThat(response.deliveryAddress()).isEqualTo(request.deliveryAddress());
        assertThat(response.recipientName()).isEqualTo(request.recipientName());
    }

    @Test
    @DisplayName("배송 생성 실패 - 이미 배송이 존재하는 주문")
    void create_delivery_fail_duplicate() {
        UUID orderId = UUID.randomUUID();

        CreateDeliveryRequest request = new CreateDeliveryRequest(
            orderId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "서울시 강남구 테헤란로 123",
            "101호",
            "홍길동",
            "U12345678",
            UUID.randomUUID(),
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );

        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "MASTER_ADMIN", null, null);

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(true);

        assertThatThrownBy(() -> deliveryService.createDelivery(request, currentUser))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.DELIVERY_ALREADY_EXISTS.getMessage());
    }

    @Test
    @DisplayName("배송 삭제 시 배송 경로 로그도 함께 soft delete")
    void delete_delivery_soft_delete_route_logs() {
        UUID deliveryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        CurrentUser currentUser = new CurrentUser(userId, "MASTER_ADMIN", null, null);

        Delivery delivery = Delivery.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "서울시 강남구 테헤란로 123",
            "101호",
            "홍길동",
            "U12345678",
            UUID.randomUUID(),
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );

        DeliveryRouteLog routeLog = DeliveryRouteLog.create(
            delivery.getId(),
            1,
            UUID.randomUUID(),
            UUID.randomUUID(),
            java.math.BigDecimal.valueOf(12.5),
            30,
            UUID.randomUUID()
        );

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
        given(deliveryRouteLogRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId))
            .willReturn(List.of(routeLog));

        deliveryService.deleteDelivery(deliveryId, currentUser);

        assertThat(delivery.getDeletedAt()).isNotNull();
        assertThat(delivery.getDeletedBy()).isEqualTo(userId);
        assertThat(routeLog.getDeletedAt()).isNotNull();
        assertThat(routeLog.getDeletedBy()).isEqualTo(userId);
    }
}
