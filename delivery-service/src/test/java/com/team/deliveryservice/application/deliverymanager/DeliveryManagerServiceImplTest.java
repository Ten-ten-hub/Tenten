package com.team.deliveryservice.application.deliverymanager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.team.deliveryservice.deliverymanager.application.dto.request.CreateDeliveryManagerRequest;
import com.team.deliveryservice.deliverymanager.application.dto.request.UpdateDeliveryManagerRequest;
import com.team.deliveryservice.deliverymanager.application.dto.response.DeliveryManagerPageResponse;
import com.team.deliveryservice.deliverymanager.application.dto.response.DeliveryManagerResponse;
import com.team.deliveryservice.deliverymanager.application.search.DeliveryManagerSearchCondition;
import com.team.deliveryservice.deliverymanager.application.service.DeliveryManagerServiceImpl;
import com.team.deliveryservice.deliverymanager.domain.DeliveryManager;
import com.team.deliveryservice.deliverymanager.domain.DeliveryManagerRepository;
import com.team.deliveryservice.deliverymanager.domain.DeliveryManagerType;
import com.team.deliveryservice.global.common.CurrentUser;
import com.team.deliveryservice.global.error.DeliveryErrorCode;
import com.team.deliveryservice.global.error.ServiceException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class DeliveryManagerServiceImplTest {

    @Mock
    private DeliveryManagerRepository deliveryManagerRepository;

    @InjectMocks
    private DeliveryManagerServiceImpl deliveryManagerService;

    @Test
    @DisplayName("배송담당자 생성 성공 - 마스터 관리자는 허브 배송 담당자 생성 가능")
    void create_delivery_manager_success_by_master() {
        UUID userId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        CreateDeliveryManagerRequest request = new CreateDeliveryManagerRequest(
            userId,
            hubId,
            "U123HUB",
            DeliveryManagerType.HUB_DELIVERY_MANAGER
        );

        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "MASTER_ADMIN", null, null);

        given(deliveryManagerRepository.findTopByTypeAndDeletedAtIsNullOrderByDeliverySequenceDesc(
            DeliveryManagerType.HUB_DELIVERY_MANAGER
        )).willReturn(Optional.empty());

        given(deliveryManagerRepository.saveAndFlush(any(DeliveryManager.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        DeliveryManagerResponse response = deliveryManagerService.createDeliveryManager(request, currentUser);

        assertThat(response.deliveryManagerId()).isEqualTo(userId);
        assertThat(response.hubId()).isEqualTo(hubId);
        assertThat(response.type()).isEqualTo(DeliveryManagerType.HUB_DELIVERY_MANAGER);
        assertThat(response.deliverySequence()).isEqualTo(0);
    }

    @Test
    @DisplayName("배송담당자 생성 성공 - 허브 관리자는 자기 허브의 업체 배송 담당자만 생성 가능")
    void create_company_delivery_manager_success_by_hub_admin() {
        UUID hubId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        CreateDeliveryManagerRequest request = new CreateDeliveryManagerRequest(
            userId,
            hubId,
            "U123COMPANY",
            DeliveryManagerType.COMPANY_DELIVERY_MANAGER
        );

        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "HUB_ADMIN", hubId, null);

        DeliveryManager lastManager = DeliveryManager.create(
            UUID.randomUUID(),
            hubId,
            "U99999999",
            DeliveryManagerType.COMPANY_DELIVERY_MANAGER,
            4
        );

        given(deliveryManagerRepository.findTopByTypeAndHubIdAndDeletedAtIsNullOrderByDeliverySequenceDesc(
            DeliveryManagerType.COMPANY_DELIVERY_MANAGER,
            hubId
        )).willReturn(Optional.of(lastManager));

        given(deliveryManagerRepository.saveAndFlush(any(DeliveryManager.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        DeliveryManagerResponse response = deliveryManagerService.createDeliveryManager(request, currentUser);

        assertThat(response.deliveryManagerId()).isEqualTo(userId);
        assertThat(response.hubId()).isEqualTo(hubId);
        assertThat(response.type()).isEqualTo(DeliveryManagerType.COMPANY_DELIVERY_MANAGER);
        assertThat(response.deliverySequence()).isEqualTo(5);
    }

    @Test
    @DisplayName("배송담당자 생성 성공 - sequence 충돌 발생 시 재시도 후 저장 성공")
    void create_delivery_manager_success_after_retry_on_sequence_conflict() {
        UUID hubId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        CreateDeliveryManagerRequest request = new CreateDeliveryManagerRequest(
            userId,
            hubId,
            "U123COMPANY",
            DeliveryManagerType.COMPANY_DELIVERY_MANAGER
        );

        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "MASTER_ADMIN", null, null);

        DeliveryManager firstLastManager = DeliveryManager.create(
            UUID.randomUUID(),
            hubId,
            "U0001",
            DeliveryManagerType.COMPANY_DELIVERY_MANAGER,
            4
        );

        DeliveryManager secondLastManager = DeliveryManager.create(
            UUID.randomUUID(),
            hubId,
            "U0002",
            DeliveryManagerType.COMPANY_DELIVERY_MANAGER,
            5
        );

        given(deliveryManagerRepository.findTopByTypeAndHubIdAndDeletedAtIsNullOrderByDeliverySequenceDesc(
            DeliveryManagerType.COMPANY_DELIVERY_MANAGER,
            hubId
        )).willReturn(Optional.of(firstLastManager), Optional.of(secondLastManager));

        AtomicInteger saveCallCount = new AtomicInteger();

        given(deliveryManagerRepository.saveAndFlush(any(DeliveryManager.class)))
            .willAnswer(invocation -> {
                if (saveCallCount.getAndIncrement() == 0) {
                    throw new DataIntegrityViolationException(
                        "duplicate key value violates unique constraint uk_p_delivery_manager"
                    );
                }
                return invocation.getArgument(0);
            });

        DeliveryManagerResponse response = deliveryManagerService.createDeliveryManager(request, currentUser);

        assertThat(response.deliveryManagerId()).isEqualTo(userId);
        assertThat(response.hubId()).isEqualTo(hubId);
        assertThat(response.type()).isEqualTo(DeliveryManagerType.COMPANY_DELIVERY_MANAGER);
        assertThat(response.deliverySequence()).isEqualTo(6);
        assertThat(saveCallCount.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("배송담당자 생성 실패 - sequence 충돌이 최대 재시도 횟수를 초과하면 예외 발생")
    void create_delivery_manager_fail_when_sequence_conflict_exceeds_retry_limit() {
        UUID hubId = UUID.randomUUID();

        CreateDeliveryManagerRequest request = new CreateDeliveryManagerRequest(
            UUID.randomUUID(),
            hubId,
            "U123COMPANY",
            DeliveryManagerType.COMPANY_DELIVERY_MANAGER
        );

        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "MASTER_ADMIN", null, null);

        DeliveryManager lastManager = DeliveryManager.create(
            UUID.randomUUID(),
            hubId,
            "U99999999",
            DeliveryManagerType.COMPANY_DELIVERY_MANAGER,
            4
        );

        given(deliveryManagerRepository.findTopByTypeAndHubIdAndDeletedAtIsNullOrderByDeliverySequenceDesc(
            DeliveryManagerType.COMPANY_DELIVERY_MANAGER,
            hubId
        )).willReturn(Optional.of(lastManager));

        given(deliveryManagerRepository.saveAndFlush(any(DeliveryManager.class)))
            .willThrow(new DataIntegrityViolationException(
                "duplicate key value violates unique constraint uk_p_delivery_manager"
            ));

        assertThatThrownBy(() -> deliveryManagerService.createDeliveryManager(request, currentUser))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.DELIVERY_MANAGER_SEQUENCE_CONFLICT.getMessage());
    }

    @Test
    @DisplayName("배송담당자 생성 실패 - 허브 관리자는 허브 배송 담당자를 생성할 수 없음")
    void create_delivery_manager_fail_hub_admin_cannot_create_hub_manager() {
        UUID hubId = UUID.randomUUID();

        CreateDeliveryManagerRequest request = new CreateDeliveryManagerRequest(
            UUID.randomUUID(),
            hubId,
            "U123HUB",
            DeliveryManagerType.HUB_DELIVERY_MANAGER
        );

        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "HUB_ADMIN", hubId, null);

        assertThatThrownBy(() -> deliveryManagerService.createDeliveryManager(request, currentUser))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.COMMON_ACCESS_DENIED.getMessage());
    }

    @Test
    @DisplayName("배송담당자 생성 실패 - 허브 관리자는 자기 허브가 아닌 업체 배송 담당자를 생성할 수 없음")
    void create_delivery_manager_fail_hub_admin_other_hub() {
        UUID adminHubId = UUID.randomUUID();
        UUID anotherHubId = UUID.randomUUID();

        CreateDeliveryManagerRequest request = new CreateDeliveryManagerRequest(
            UUID.randomUUID(),
            anotherHubId,
            "U123COMPANY",
            DeliveryManagerType.COMPANY_DELIVERY_MANAGER
        );

        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "HUB_ADMIN", adminHubId, null);

        assertThatThrownBy(() -> deliveryManagerService.createDeliveryManager(request, currentUser))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.COMMON_ACCESS_DENIED.getMessage());
    }

    @Test
    @DisplayName("배송담당자 단건 조회 성공 - 마스터 관리자")
    void get_delivery_manager_success_by_master() {
        UUID deliveryManagerId = UUID.randomUUID();
        DeliveryManager manager = createCompanyDeliveryManager(deliveryManagerId, UUID.randomUUID(), 1);

        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "MASTER_ADMIN", null, null);

        given(deliveryManagerRepository.findByIdAndDeletedAtIsNull(deliveryManagerId))
            .willReturn(Optional.of(manager));

        DeliveryManagerResponse response = deliveryManagerService.getDeliveryManager(deliveryManagerId, currentUser);

        assertThat(response.deliveryManagerId()).isEqualTo(deliveryManagerId);
        assertThat(response.type()).isEqualTo(DeliveryManagerType.COMPANY_DELIVERY_MANAGER);
    }

    @Test
    @DisplayName("배송담당자 단건 조회 성공 - 배송 담당자 본인")
    void get_delivery_manager_success_by_self() {
        UUID deliveryManagerId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        DeliveryManager manager = createHubDeliveryManager(deliveryManagerId, hubId, 3);

        CurrentUser currentUser = new CurrentUser(deliveryManagerId, "HUB_DELIVERY_MANAGER", hubId, null);

        given(deliveryManagerRepository.findByIdAndDeletedAtIsNull(deliveryManagerId))
            .willReturn(Optional.of(manager));

        DeliveryManagerResponse response = deliveryManagerService.getDeliveryManager(deliveryManagerId, currentUser);

        assertThat(response.deliveryManagerId()).isEqualTo(deliveryManagerId);
    }

    @Test
    @DisplayName("배송담당자 단건 조회 실패 - 배송 담당자는 본인 정보만 조회 가능")
    void get_delivery_manager_fail_not_self() {
        UUID deliveryManagerId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        DeliveryManager manager = createHubDeliveryManager(deliveryManagerId, hubId, 3);

        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "HUB_DELIVERY_MANAGER", hubId, null);

        given(deliveryManagerRepository.findByIdAndDeletedAtIsNull(deliveryManagerId))
            .willReturn(Optional.of(manager));

        assertThatThrownBy(() -> deliveryManagerService.getDeliveryManager(deliveryManagerId, currentUser))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.COMMON_ACCESS_DENIED.getMessage());
    }

    @Test
    @DisplayName("배송담당자 검색 성공 - 허브 관리자는 자기 허브 소속만 조회")
    void search_delivery_managers_success_by_hub_admin() {
        UUID hubId = UUID.randomUUID();
        DeliveryManager manager1 = createCompanyDeliveryManager(UUID.randomUUID(), hubId, 0);
        DeliveryManager manager2 = createCompanyDeliveryManager(UUID.randomUUID(), hubId, 1);

        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "HUB_ADMIN", hubId, null);

        DeliveryManagerSearchCondition condition = new DeliveryManagerSearchCondition(
            null,
            null,
            0,
            10,
            "deliverySequence",
            "ASC"
        );

        given(deliveryManagerRepository.search(any(), any(), any()))
            .willReturn(new PageImpl<>(
                List.of(manager1, manager2),
                PageRequest.of(0, 10),
                2
            ));

        DeliveryManagerPageResponse response =
            deliveryManagerService.searchDeliveryManagers(condition, currentUser);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).hubId()).isEqualTo(hubId);
        assertThat(response.content().get(0).deliverySequence()).isEqualTo(0);
        assertThat(response.content().get(1).deliverySequence()).isEqualTo(1);
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("배송담당자 검색 성공 - size가 10,30,50 외 값이면 10으로 보정")
    void search_delivery_managers_size_normalized() {
        UUID hubId = UUID.randomUUID();
        DeliveryManager manager = createCompanyDeliveryManager(UUID.randomUUID(), hubId, 0);

        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "MASTER_ADMIN", null, null);

        DeliveryManagerSearchCondition condition = new DeliveryManagerSearchCondition(
            null,
            null,
            0,
            7,
            "createdAt",
            "DESC"
        );

        given(deliveryManagerRepository.search(any(), any(), any()))
            .willReturn(new PageImpl<>(
                List.of(manager),
                PageRequest.of(0, 10),
                1
            ));

        DeliveryManagerPageResponse response =
            deliveryManagerService.searchDeliveryManagers(condition, currentUser);

        assertThat(response.size()).isEqualTo(10);
    }

    @Test
    @DisplayName("배송담당자 수정 성공 - 마스터 관리자")
    void update_delivery_manager_success_by_master() {
        UUID deliveryManagerId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        DeliveryManager manager = createCompanyDeliveryManager(deliveryManagerId, hubId, 1);

        UpdateDeliveryManagerRequest request = new UpdateDeliveryManagerRequest(
            hubId,
            "UNEWSLACK",
            DeliveryManagerType.COMPANY_DELIVERY_MANAGER
        );

        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "MASTER_ADMIN", null, null);

        given(deliveryManagerRepository.findByIdAndDeletedAtIsNull(deliveryManagerId))
            .willReturn(Optional.of(manager));

        DeliveryManagerResponse response =
            deliveryManagerService.updateDeliveryManager(deliveryManagerId, request, currentUser);

        assertThat(response.slackId()).isEqualTo("UNEWSLACK");
        assertThat(response.hubId()).isEqualTo(hubId);
        assertThat(response.type()).isEqualTo(DeliveryManagerType.COMPANY_DELIVERY_MANAGER);
    }

    @Test
    @DisplayName("배송담당자 수정 성공 - 허브 관리자는 자기 허브의 업체 배송 담당자만 수정 가능")
    void update_delivery_manager_success_by_hub_admin() {
        UUID hubId = UUID.randomUUID();
        UUID deliveryManagerId = UUID.randomUUID();

        DeliveryManager manager = createCompanyDeliveryManager(deliveryManagerId, hubId, 1);

        UpdateDeliveryManagerRequest request = new UpdateDeliveryManagerRequest(
            hubId,
            "UCHANGED",
            DeliveryManagerType.COMPANY_DELIVERY_MANAGER
        );

        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "HUB_ADMIN", hubId, null);

        given(deliveryManagerRepository.findByIdAndDeletedAtIsNull(deliveryManagerId))
            .willReturn(Optional.of(manager));

        DeliveryManagerResponse response =
            deliveryManagerService.updateDeliveryManager(deliveryManagerId, request, currentUser);

        assertThat(response.slackId()).isEqualTo("UCHANGED");
    }

    @Test
    @DisplayName("배송담당자 수정 실패 - 허브 관리자는 허브 배송 담당자를 수정할 수 없음")
    void update_delivery_manager_fail_hub_admin_cannot_update_hub_manager() {
        UUID hubId = UUID.randomUUID();
        UUID deliveryManagerId = UUID.randomUUID();

        DeliveryManager manager = createHubDeliveryManager(deliveryManagerId, hubId, 1);

        UpdateDeliveryManagerRequest request = new UpdateDeliveryManagerRequest(
            hubId,
            "UCHANGED",
            DeliveryManagerType.HUB_DELIVERY_MANAGER
        );

        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "HUB_ADMIN", hubId, null);

        given(deliveryManagerRepository.findByIdAndDeletedAtIsNull(deliveryManagerId))
            .willReturn(Optional.of(manager));

        assertThatThrownBy(() -> deliveryManagerService.updateDeliveryManager(deliveryManagerId, request, currentUser))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.COMMON_ACCESS_DENIED.getMessage());
    }

    @Test
    @DisplayName("배송담당자 삭제 성공 - 마스터 관리자")
    void delete_delivery_manager_success_by_master() {
        UUID deliveryManagerId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        DeliveryManager manager = createCompanyDeliveryManager(deliveryManagerId, UUID.randomUUID(), 1);

        CurrentUser currentUser = new CurrentUser(actorId, "MASTER_ADMIN", null, null);

        given(deliveryManagerRepository.findByIdAndDeletedAtIsNull(deliveryManagerId))
            .willReturn(Optional.of(manager));

        deliveryManagerService.deleteDeliveryManager(deliveryManagerId, currentUser);

        assertThat(manager.getDeletedAt()).isNotNull();
        assertThat(manager.getDeletedBy()).isEqualTo(actorId);
    }

    @Test
    @DisplayName("배송담당자 삭제 성공 - 허브 관리자는 자기 허브의 업체 배송 담당자만 삭제 가능")
    void delete_delivery_manager_success_by_hub_admin() {
        UUID deliveryManagerId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        DeliveryManager manager = createCompanyDeliveryManager(deliveryManagerId, hubId, 1);

        CurrentUser currentUser = new CurrentUser(actorId, "HUB_ADMIN", hubId, null);

        given(deliveryManagerRepository.findByIdAndDeletedAtIsNull(deliveryManagerId))
            .willReturn(Optional.of(manager));

        deliveryManagerService.deleteDeliveryManager(deliveryManagerId, currentUser);

        assertThat(manager.getDeletedAt()).isNotNull();
        assertThat(manager.getDeletedBy()).isEqualTo(actorId);
    }

    @Test
    @DisplayName("배송담당자 삭제 실패 - 허브 관리자는 허브 배송 담당자를 삭제할 수 없음")
    void delete_delivery_manager_fail_hub_admin_cannot_delete_hub_manager() {
        UUID deliveryManagerId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        DeliveryManager manager = createHubDeliveryManager(deliveryManagerId, hubId, 1);
        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "HUB_ADMIN", hubId, null);

        given(deliveryManagerRepository.findByIdAndDeletedAtIsNull(deliveryManagerId))
            .willReturn(Optional.of(manager));

        assertThatThrownBy(() -> deliveryManagerService.deleteDeliveryManager(deliveryManagerId, currentUser))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.COMMON_ACCESS_DENIED.getMessage());
    }

    private DeliveryManager createCompanyDeliveryManager(UUID id, UUID hubId, int sequence) {
        return DeliveryManager.create(
            id,
            hubId,
            "U123COMPANY",
            DeliveryManagerType.COMPANY_DELIVERY_MANAGER,
            sequence
        );
    }

    private DeliveryManager createHubDeliveryManager(UUID id, UUID hubId, int sequence) {
        return DeliveryManager.create(
            id,
            hubId,
            "U123HUB",
            DeliveryManagerType.HUB_DELIVERY_MANAGER,
            sequence
        );
    }
}
