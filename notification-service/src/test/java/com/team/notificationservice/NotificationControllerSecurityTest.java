package com.team.notificationservice;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.notificationservice.domain.Notification;
import com.team.notificationservice.domain.NotificationRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.config.import=optional:file:../application-common.properties,optional:file:../application-secret.properties"
})
@AutoConfigureMockMvc
class NotificationControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("권한이 없는 유저가 삭제 요청 시 403 에러가 발생한다")
    void delete_Forbidden_WhenNotMasterAdmin() throws Exception {
        // given
        UUID notificationId = UUID.randomUUID();

        // when & then
        mockMvc.perform(delete("/api/v1/notifications/{id}", notificationId)
                .header("X-User-Role", "USER") // 잘못된 권한 (MASTER_ADMIN이 아님)
                .header("X-User-Id", UUID.randomUUID().toString()))
            .andExpect(status().isForbidden()); // 403 Forbidden 응답 확인
    }

    @Test
    @DisplayName("MASTER_ADMIN 권한을 가진 유저가 삭제 요청 시 성공한다")
    void delete_Success_WhenMasterAdmin() throws Exception {
        // given
        UUID notificationId = UUID.randomUUID();
        Notification mockNoti = Notification.builder().msgContent("삭제 테스트").build();

        given(notificationRepository.findByIdAndDeletedAtIsNull(notificationId))
            .willReturn(Optional.of(mockNoti));

        // [추가] save 호출 시의 Stubbing 추가
        given(notificationRepository.save(any(Notification.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        // when & then
        mockMvc.perform(delete("/api/v1/notifications/{id}", notificationId)
                .header("X-User-Role", "MASTER_ADMIN")
                .header("X-User-Id", UUID.randomUUID().toString()))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("MASTER_ADMIN 권한 삭제 시 실제 DB 반영 여부를 검증한다")
    void delete_VerifyExecution_WhenMasterAdmin() throws Exception {
        // given
        UUID notificationId = UUID.randomUUID();
        Notification mockNoti = spy(Notification.builder().msgContent("삭제").build());

        given(notificationRepository.findByIdAndDeletedAtIsNull(notificationId))
            .willReturn(Optional.of(mockNoti));
        given(notificationRepository.save(any(Notification.class)))
            .willAnswer(inv -> inv.getArgument(0));

        // when
        mockMvc.perform(delete("/api/v1/notifications/{id}", notificationId)
                .header("X-User-Role", "MASTER_ADMIN")
                .header("X-User-Id", UUID.randomUUID().toString()))
            .andExpect(status().isOk());

        // then
        verify(notificationRepository).save(any());
        assertNotNull(mockNoti.getDeletedAt());
    }

    @Test
    @DisplayName("X-User-Role 헤더가 없으면 401 에러를 반환한다")
    void delete_Unauthorized_WhenRoleHeaderMissing() throws Exception {
        mockMvc.perform(delete("/api/v1/notifications/{id}", UUID.randomUUID())
                .header("X-User-Id", UUID.randomUUID().toString()))
            .andExpect(status().isUnauthorized());
    }
}
