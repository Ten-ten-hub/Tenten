package com.team.companyservice.application.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.team.companyservice.domain.company.Company;
import com.team.companyservice.domain.company.CompanyRepository;
import com.team.companyservice.domain.company.CompanyType;
import com.team.companyservice.infrastructure.client.HubClient;
import com.team.companyservice.infrastructure.client.HubExistsResponse;
import com.team.companyservice.presentation.common.CompanyErrorCode;
import com.team.companyservice.presentation.common.CurrentUser;
import com.team.companyservice.presentation.common.ServiceException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private HubClient hubClient;

    @InjectMocks
    private CompanyService companyService;

    @Test
    @DisplayName("업체 생성 성공")
    void create_success() {
        UUID hubId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        CreateCompanyRequest request = new CreateCompanyRequestFixture(
            "서울 생산업체",
            CompanyType.PRODUCER,
            hubId,
            "서울시 강남구 테헤란로 123",
            "5층",
            "06234",
            "홍길동",
            "010-1234-5678",
            "U123456"
        ).toRequest();

        CurrentUser currentUser = new CurrentUser(userId, "MASTER_ADMIN", null, null);

        given(hubClient.existsHub(hubId)).willReturn(
            new HubExistsResponse(
                true,
                new HubExistsResponse.HubExistsData(hubId, true),
                "SUCCESS",
                "허브 조회 성공"
            )
        );
        given(companyRepository.existsByHubIdAndNameAndDeletedAtIsNull(hubId, "서울 생산업체")).willReturn(false);
        given(companyRepository.save(any(Company.class))).willAnswer(invocation -> invocation.getArgument(0));

        CompanyResponse response = companyService.create(request, currentUser);

        assertThat(response.name()).isEqualTo("서울 생산업체");
        assertThat(response.companyType()).isEqualTo(CompanyType.PRODUCER);
        assertThat(response.hubId()).isEqualTo(hubId);
        assertThat(response.isActive()).isTrue();
    }

    @Test
    @DisplayName("업체 생성 실패 - 허브가 존재하지 않음")
    void create_fail_hub_not_found() {
        UUID hubId = UUID.randomUUID();

        CreateCompanyRequest request = new CreateCompanyRequestFixture(
            "서울 생산업체",
            CompanyType.PRODUCER,
            hubId,
            "서울시 강남구 테헤란로 123",
            null,
            null,
            null,
            null,
            null
        ).toRequest();

        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "MASTER_ADMIN", null, null);

        given(hubClient.existsHub(hubId)).willReturn(
            new HubExistsResponse(
                true,
                new HubExistsResponse.HubExistsData(hubId, false),
                "SUCCESS",
                "허브 조회 성공"
            )
        );

        assertThatThrownBy(() -> companyService.create(request, currentUser))
            .isInstanceOf(ServiceException.class)
            .hasMessage(CompanyErrorCode.HUB_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("업체 생성 실패 - 같은 허브 내 업체명 중복")
    void create_fail_duplicate_name() {
        UUID hubId = UUID.randomUUID();

        CreateCompanyRequest request = new CreateCompanyRequestFixture(
            "서울 생산업체",
            CompanyType.PRODUCER,
            hubId,
            "서울시 강남구 테헤란로 123",
            null,
            null,
            null,
            null,
            null
        ).toRequest();

        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "MASTER_ADMIN", null, null);

        given(hubClient.existsHub(hubId)).willReturn(
            new HubExistsResponse(
                true,
                new HubExistsResponse.HubExistsData(hubId, true),
                "SUCCESS",
                "허브 조회 성공"
            )
        );
        given(companyRepository.existsByHubIdAndNameAndDeletedAtIsNull(hubId, "서울 생산업체")).willReturn(true);

        assertThatThrownBy(() -> companyService.create(request, currentUser))
            .isInstanceOf(ServiceException.class)
            .hasMessage(CompanyErrorCode.COMPANY_DUPLICATED.getMessage());
    }

    static class CreateCompanyRequestFixture {
        private final String name;
        private final CompanyType companyType;
        private final UUID hubId;
        private final String address;
        private final String addressDetail;
        private final String zipcode;
        private final String contactName;
        private final String contactPhone;
        private final String contactSlackId;

        CreateCompanyRequestFixture(
            String name,
            CompanyType companyType,
            UUID hubId,
            String address,
            String addressDetail,
            String zipcode,
            String contactName,
            String contactPhone,
            String contactSlackId
        ) {
            this.name = name;
            this.companyType = companyType;
            this.hubId = hubId;
            this.address = address;
            this.addressDetail = addressDetail;
            this.zipcode = zipcode;
            this.contactName = contactName;
            this.contactPhone = contactPhone;
            this.contactSlackId = contactSlackId;
        }

        CreateCompanyRequest toRequest() {
            CreateCompanyRequest request = new CreateCompanyRequest();
            setField(request, "name", name);
            setField(request, "companyType", companyType);
            setField(request, "hubId", hubId);
            setField(request, "address", address);
            setField(request, "addressDetail", addressDetail);
            setField(request, "zipcode", zipcode);
            setField(request, "contactName", contactName);
            setField(request, "contactPhone", contactPhone);
            setField(request, "contactSlackId", contactSlackId);
            return request;
        }

        private void setField(Object target, String fieldName, Object value) {
            try {
                var field = target.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
