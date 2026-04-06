package com.team.deliveryservice.presentation.delivery;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.deliveryservice.delivery.application.service.DeliveryService;
import com.team.deliveryservice.delivery.presentation.ExternalDeliveryController;
import com.team.deliveryservice.delivery.presentation.InternalDeliveryController;
import com.team.deliveryservice.global.config.AuthenticationFilter;
import com.team.deliveryservice.global.config.CurrentUserArgumentResolver;
import com.team.deliveryservice.global.config.CurrentUserResolverConfig;
import com.team.deliveryservice.global.config.SecurityConfig;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {
    ExternalDeliveryController.class,
    InternalDeliveryController.class
})
@Import({
    SecurityConfig.class,
    AuthenticationFilter.class,
    CurrentUserArgumentResolver.class,
    CurrentUserResolverConfig.class
})
class DeliverySecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeliveryService deliveryService;

    @Test
    @DisplayName("인증 헤더 없이 외부 배송 조회 요청 시 401")
    void getDelivery_withoutAuthHeader_returns401() throws Exception {
        UUID deliveryId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/deliveries/{deliveryId}", deliveryId))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("허브 관리자가 외부 배송 조회 요청 시 200")
    void getDelivery_withHubAdmin_returns200() throws Exception {
        UUID deliveryId = UUID.randomUUID();
        when(deliveryService.getDelivery(any(UUID.class), any())).thenReturn(null);

        mockMvc.perform(get("/api/v1/deliveries/{deliveryId}", deliveryId)
                .header("X-User-Id", UUID.randomUUID().toString())
                .header("X-User-Role", "HUB_ADMIN")
                .header("X-Hub-Id", UUID.randomUUID().toString()))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("업체 담당자가 배송 담당자 배정 요청 시 403")
    void assignDeliveryManager_withCompanyManager_returns403() throws Exception {
        UUID deliveryId = UUID.randomUUID();

        String requestBody = """
            {
              "routeLogId": "%s",
              "deliveryManagerId": "%s"
            }
            """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(patch("/api/v1/deliveries/{deliveryId}/assign-delivery-manager", deliveryId)
                .header("X-User-Id", UUID.randomUUID().toString())
                .header("X-User-Role", "COMPANY_MANAGER")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("마스터 관리자가 외부 배송 수정 요청 시 200")
    void updateDelivery_withMasterAdmin_returns200() throws Exception {
        UUID deliveryId = UUID.randomUUID();
        when(deliveryService.updateDelivery(any(UUID.class), any(), any())).thenReturn(null);

        String requestBody = """
            {
              "deliveryAddress": "서울시 송파구",
              "deliveryAddressDetail": "101동 1001호",
              "recipientName": "홍길동",
              "recipientSlackId": "U12345678"
            }
            """;

        mockMvc.perform(put("/api/v1/deliveries/{deliveryId}", deliveryId)
                .header("X-User-Id", UUID.randomUUID().toString())
                .header("X-User-Role", "MASTER_ADMIN")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내부 배송 조회 API는 인증 없이 호출 가능")
    void internalGet_permitAll() throws Exception {
        UUID deliveryId = UUID.randomUUID();
        when(deliveryService.getDelivery(any(UUID.class))).thenReturn(null);

        mockMvc.perform(get("/internal/v1/deliveries/{deliveryId}", deliveryId))
            .andExpect(status().isOk());
    }
}
