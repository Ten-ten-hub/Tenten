package com.team.notificationservice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.notificationservice.application.NotificationService;
import com.team.notificationservice.presentation.NotificationController;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationController.class)
@ImportAutoConfiguration(exclude = {
    org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class,
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
    SecurityAutoConfiguration.class,
    SecurityFilterAutoConfiguration.class
})
class NotificationControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    @DisplayName("MASTER_ADMIN 권한을 가진 유저가 삭제 요청 시 성공한다")
    void delete_Success_WhenMasterAdmin() throws Exception {
        UUID notificationId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/notifications/{id}", notificationId)
                .header("X-User-Role", "MASTER_ADMIN")
                .header("X-User-Id", UUID.randomUUID().toString()))
            .andExpect(status().isOk());

        verify(notificationService).deleteNotification(any(UUID.class), anyString());
    }
}
