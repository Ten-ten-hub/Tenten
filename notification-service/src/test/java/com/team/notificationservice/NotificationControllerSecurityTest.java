package com.team.notificationservice;

import static org.mockito.Mockito.when;
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

        // ID 조회가 성공하도록 설정
        when(notificationRepository.findByIdAndDeletedAtIsNull(notificationId))
            .thenReturn(Optional.of(mockNoti));

        // when & then
        mockMvc.perform(delete("/api/v1/notifications/{id}", notificationId)
                .header("X-User-Role", "MASTER_ADMIN")
                .header("X-User-Id", UUID.randomUUID().toString()))
            .andExpect(status().isOk());
    }
}
