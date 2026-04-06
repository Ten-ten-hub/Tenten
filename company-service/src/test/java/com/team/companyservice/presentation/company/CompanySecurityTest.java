package com.team.companyservice.presentation.company;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.companyservice.company.application.service.CompanyService;
import com.team.companyservice.company.presentation.ExternalCompanyController;
import com.team.companyservice.company.presentation.InternalCompanyController;
import com.team.companyservice.global.config.AuthenticationFilter;
import com.team.companyservice.global.config.CurrentUserArgumentResolver;
import com.team.companyservice.global.config.CurrentUserResolverConfig;
import com.team.companyservice.global.config.SecurityConfig;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {
    ExternalCompanyController.class,
    InternalCompanyController.class
})
@Import({
    SecurityConfig.class,
    AuthenticationFilter.class,
    CurrentUserArgumentResolver.class,
    CurrentUserResolverConfig.class
})
class CompanySecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompanyService companyService;

    @Test
    @DisplayName("인증 헤더 없이 외부 업체 삭제 요청 시 401")
    void deleteCompany_withoutAuthHeader_returns401() throws Exception {
        UUID companyId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/companies/{companyId}", companyId))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("업체 담당자가 외부 업체 삭제 요청 시 403")
    void deleteCompany_withCompanyManager_returns403() throws Exception {
        UUID companyId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/companies/{companyId}", companyId)
                .header("X-User-Id", UUID.randomUUID().toString())
                .header("X-User-Role", "COMPANY_MANAGER"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("마스터 관리자가 외부 업체 삭제 요청 시 200")
    void deleteCompany_withMasterAdmin_returns200() throws Exception {
        UUID companyId = UUID.randomUUID();

        doNothing().when(companyService).delete(any(UUID.class), any());

        mockMvc.perform(delete("/api/v1/companies/{companyId}", companyId)
                .header("X-User-Id", UUID.randomUUID().toString())
                .header("X-User-Role", "MASTER_ADMIN"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("허브 관리자가 외부 업체 생성 요청 시 201")
    void createCompany_withHubAdmin_returns201() throws Exception {
        when(companyService.create(any(), any())).thenReturn(null);

        String requestBody = """
            {
              "name": "테스트 업체",
              "companyType": "PRODUCER",
              "hubId": "%s",
              "address": "서울시 강남구"
            }
            """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/companies")
                .header("X-User-Id", UUID.randomUUID().toString())
                .header("X-User-Role", "HUB_ADMIN")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("내부 업체 존재 확인 API는 인증 없이 호출 가능")
    void internalExists_permitAll() throws Exception {
        UUID companyId = UUID.randomUUID();
        when(companyService.get(any(UUID.class))).thenReturn(null);

        mockMvc.perform(get("/internal/v1/companies/{companyId}/exists", companyId))
            .andExpect(status().isOk());
    }
}
