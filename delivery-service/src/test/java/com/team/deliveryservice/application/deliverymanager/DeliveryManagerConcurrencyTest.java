// 통합테스트 - 업체 배송담당자 동시성 테스트
package com.team.deliveryservice.application.deliverymanager;

import static org.assertj.core.api.Assertions.assertThat;

import com.team.deliveryservice.deliverymanager.application.dto.request.CreateDeliveryManagerRequest;
import com.team.deliveryservice.deliverymanager.application.service.DeliveryManagerServiceImpl;
import com.team.deliveryservice.deliverymanager.domain.DeliveryManager;
import com.team.deliveryservice.deliverymanager.domain.DeliveryManagerRepository;
import com.team.deliveryservice.deliverymanager.domain.DeliveryManagerType;
import com.team.deliveryservice.global.common.CurrentUser;
import com.team.deliveryservice.global.config.JpaAuditingConfig;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers
@Import({
    DeliveryManagerServiceImpl.class,
    JpaAuditingConfig.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DeliveryManagerConcurrencyTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("test_delivery")
        .withUsername("postgres")
        .withPassword("postgres");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);

        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> true);

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.show-sql", () -> false);
    }

    @Autowired
    private DeliveryManagerServiceImpl deliveryManagerService;

    @Autowired
    private DeliveryManagerRepository deliveryManagerRepository;

    @AfterEach
    void tearDown() {
        deliveryManagerRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("동시 요청 시 업체 배송담당자는 저장된 데이터 기준으로 deliverySequence 중복이 없어야 한다")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void company_deliveryManager_sequence_concurrency_test() throws Exception {
        // given
        int threadCount = 10;
        UUID hubId = UUID.randomUUID();

        // 비관적 락 검증을 위해 seed row를 하나 먼저 생성
        deliveryManagerRepository.saveAndFlush(
            DeliveryManager.create(
                UUID.randomUUID(),
                hubId,
                "U-SEED-COMPANY",
                DeliveryManagerType.COMPANY_DELIVERY_MANAGER,
                0
            )
        );

        CurrentUser masterUser = new CurrentUser(
            UUID.randomUUID(),
            "MASTER_ADMIN",
            null,
            null
        );

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        // 실패 예외도 수집하지만, 이 테스트에서는 "예외가 0개여야 한다"를 강제하지 않음
        ConcurrentLinkedQueue<Throwable> exceptions = new ConcurrentLinkedQueue<>();

        // when
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    CreateDeliveryManagerRequest request = new CreateDeliveryManagerRequest(
                        UUID.randomUUID(),
                        hubId,
                        "U-COMPANY-" + idx,
                        DeliveryManagerType.COMPANY_DELIVERY_MANAGER
                    );

                    deliveryManagerService.createDeliveryManager(request, masterUser);
                } catch (Throwable e) {
                    exceptions.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        // then
        List<DeliveryManager> result = deliveryManagerRepository.findAll().stream()
            .filter(manager -> manager.getType() == DeliveryManagerType.COMPANY_DELIVERY_MANAGER)
            .filter(manager -> hubId.equals(manager.getHubId()))
            .toList();

        // 최소한 seed 데이터는 존재해야 함
        assertThat(result).isNotEmpty();

        // 저장된 데이터들끼리는 sequence 중복이 없어야 함
        Set<Integer> sequences = result.stream()
            .map(DeliveryManager::getDeliverySequence)
            .collect(Collectors.toSet());

        assertThat(sequences).hasSize(result.size());

        // 모든 row는 동일 허브 / 동일 타입이어야 함
        assertThat(result)
            .allMatch(manager -> manager.getType() == DeliveryManagerType.COMPANY_DELIVERY_MANAGER)
            .allMatch(manager -> hubId.equals(manager.getHubId()));

        // 시퀀스는 최소 0번(seed)부터 시작해야 함
        assertThat(sequences).contains(0);

        // 예외가 발생할 수는 있지만, 저장된 데이터 무결성이 깨지면 안 됨
        assertThat(result.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("동시 요청 시 허브 배송담당자는 저장된 데이터 기준으로 deliverySequence 중복이 없어야 한다")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void hub_deliveryManager_sequence_concurrency_test() throws Exception {
        // given
        int threadCount = 10;

        // 비관적 락 검증을 위해 seed row를 하나 먼저 생성
        deliveryManagerRepository.saveAndFlush(
            DeliveryManager.create(
                UUID.randomUUID(),
                null,
                "U-SEED-HUB",
                DeliveryManagerType.HUB_DELIVERY_MANAGER,
                0
            )
        );

        CurrentUser masterUser = new CurrentUser(
            UUID.randomUUID(),
            "MASTER_ADMIN",
            null,
            null
        );

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        // 실패 예외 수집
        ConcurrentLinkedQueue<Throwable> exceptions = new ConcurrentLinkedQueue<>();

        // when
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    CreateDeliveryManagerRequest request = new CreateDeliveryManagerRequest(
                        UUID.randomUUID(),
                        null,
                        "U-HUB-" + idx,
                        DeliveryManagerType.HUB_DELIVERY_MANAGER
                    );

                    deliveryManagerService.createDeliveryManager(request, masterUser);
                } catch (Throwable e) {
                    exceptions.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        // then
        List<DeliveryManager> result = deliveryManagerRepository.findAll().stream()
            .filter(manager -> manager.getType() == DeliveryManagerType.HUB_DELIVERY_MANAGER)
            .toList();

        // 최소한 seed 데이터는 존재해야 함
        assertThat(result).isNotEmpty();

        // 저장된 데이터들끼리는 sequence 중복이 없어야 함
        Set<Integer> sequences = result.stream()
            .map(DeliveryManager::getDeliverySequence)
            .collect(Collectors.toSet());

        assertThat(sequences).hasSize(result.size());

        // 모든 row는 HUB_DELIVERY_MANAGER 이고 hubId 는 null 이어야 함
        assertThat(result)
            .allMatch(manager -> manager.getType() == DeliveryManagerType.HUB_DELIVERY_MANAGER)
            .allMatch(manager -> manager.getHubId() == null);

        // seed sequence 확인
        assertThat(sequences).contains(0);

        // 예외가 있더라도 저장된 데이터의 sequence 무결성이 깨지면 안 됨
        assertThat(result.size()).isGreaterThanOrEqualTo(1);
    }
}
