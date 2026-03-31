package com.team.deliveryservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.team.deliveryservice.application.delivery.CreateDeliveryRequest;
import com.team.deliveryservice.application.delivery.DeliveryResponse;
import com.team.deliveryservice.application.delivery.DeliveryService;
import com.team.deliveryservice.domain.delivery.Delivery;
import com.team.deliveryservice.domain.delivery.DeliveryRepository;
import com.team.deliveryservice.domain.delivery.DeliveryRouteLogRepository;
import com.team.deliveryservice.domain.delivery.DeliveryStatus;
import com.team.deliveryservice.presentation.common.CurrentUser;
import com.team.deliveryservice.presentation.common.ErrorCode;
import com.team.deliveryservice.presentation.common.ServiceException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.cloud.discovery.enabled=false",
    "eureka.client.enabled=false",
    "spring.docker.compose.enabled=false",
    "management.tracing.enabled=false"
})
class DeliveryServiceApplicationTests {

    @Autowired
    private DeliveryService deliveryService;

    // Mock repository to avoid real DB access during service tests
    @MockitoBean
    private DeliveryRepository deliveryRepository;

    @MockitoBean
    private DeliveryRouteLogRepository deliveryRouteLogRepository;

    /**
     * Returns a mock current user for authorization-related service parameters.
     */
    private CurrentUser currentUser() {
        return new CurrentUser(
            UUID.randomUUID(),
            "MASTER_ADMIN",
            UUID.randomUUID(),
            UUID.randomUUID()
        );
    }

    @Test
    @DisplayName("배송 생성 성공")
    void createDeliveryTest() {
        // given: create request data
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
            LocalDateTime.now().plusDays(1)
        );

        // given: no existing delivery for the order
        when(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(request.orderId()))
            .thenReturn(false);

        // given: mock saved delivery entity
        Delivery delivery = Delivery.create(
            request.orderId(),
            request.originHubId(),
            request.destinationHubId(),
            request.receiverCompanyId(),
            request.deliveryAddress(),
            request.deliveryAddressDetail(),
            request.recipientName(),
            request.recipientSlackId(),
            request.companyDeliveryManagerId(),
            request.finalDispatchDeadlineAt()
        );

        when(deliveryRepository.save(org.mockito.ArgumentMatchers.any(Delivery.class)))
            .thenReturn(delivery);

        // when: create delivery service is called
        DeliveryResponse response = deliveryService.createDelivery(request, currentUser());

        // then: response should contain created delivery data
        assertNotNull(response);
        assertEquals(request.orderId(), response.orderId());
        assertEquals(request.deliveryAddress(), response.deliveryAddress());
        assertEquals(DeliveryStatus.WAITING_AT_HUB, response.deliveryStatus());
    }

    @Test
    @DisplayName("배송 단건 조회 성공")
    void getDeliveryTest() {
        // given: delivery id
        UUID deliveryId = UUID.randomUUID();

        // given: mock delivery entity
        Delivery delivery = Delivery.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "서울시 송파구 올림픽로 1",
            "202호",
            "김민지",
            "U87654321",
            UUID.randomUUID(),
            LocalDateTime.now().plusDays(2)
        );

        when(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId))
            .thenReturn(Optional.of(delivery));

        // when: get delivery by id
        DeliveryResponse response = deliveryService.getDelivery(deliveryId, currentUser());

        // then: response should match mock delivery data
        assertNotNull(response);
        assertEquals(delivery.getOrderId(), response.orderId());
        assertEquals(delivery.getRecipientName(), response.recipientName());
    }

    @Test
    @DisplayName("존재하지 않는 배송 조회 시 예외 발생")
    void getDeliveryNotFoundTest() {
        // given: unknown delivery id
        UUID deliveryId = UUID.randomUUID();

        when(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId))
            .thenReturn(Optional.empty());

        // when & then: service should throw DELIVERY_NOT_FOUND exception
        ServiceException exception = assertThrows(
            ServiceException.class,
            () -> deliveryService.getDelivery(deliveryId, currentUser())
        );

        assertEquals(ErrorCode.DELIVERY_NOT_FOUND, exception.getErrorCode());
    }
}
