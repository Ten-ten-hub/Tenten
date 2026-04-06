package com.team.deliveryservice.application.deliverymanager;

import static org.assertj.core.api.Assertions.assertThat;

import com.team.deliveryservice.deliverymanager.application.dto.request.CreateDeliveryManagerRequest;
import com.team.deliveryservice.deliverymanager.application.service.DeliveryManagerServiceImpl;
import com.team.deliveryservice.deliverymanager.domain.DeliveryManager;
import com.team.deliveryservice.deliverymanager.domain.DeliveryManagerRepository;
import com.team.deliveryservice.deliverymanager.domain.DeliveryManagerType;
import com.team.deliveryservice.global.common.CurrentUser;
import com.team.deliveryservice.global.config.JpaAuditingConfig;
import com.team.deliveryservice.support.PostgreSQLTestSupport;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import({
    DeliveryManagerServiceImpl.class,
    JpaAuditingConfig.class
})
class DeliveryManagerConcurrencyTest extends PostgreSQLTestSupport {

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
        int threadCount = 10;
        UUID hubId = UUID.randomUUID();

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

        ConcurrentLinkedQueue<Throwable> exceptions = new ConcurrentLinkedQueue<>();

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

        List<DeliveryManager> result = deliveryManagerRepository.findAll().stream()
            .filter(manager -> manager.getType() == DeliveryManagerType.COMPANY_DELIVERY_MANAGER)
            .filter(manager -> hubId.equals(manager.getHubId()))
            .toList();

        assertThat(result).isNotEmpty();

        Set<Integer> sequences = result.stream()
            .map(DeliveryManager::getDeliverySequence)
            .collect(Collectors.toSet());

        assertThat(sequences).hasSize(result.size());
        assertThat(result)
            .allMatch(manager -> manager.getType() == DeliveryManagerType.COMPANY_DELIVERY_MANAGER)
            .allMatch(manager -> hubId.equals(manager.getHubId()));
        assertThat(sequences).contains(0);
        assertThat(result.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("동시 요청 시 허브 배송담당자는 저장된 데이터 기준으로 deliverySequence 중복이 없어야 한다")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void hub_deliveryManager_sequence_concurrency_test() throws Exception {
        int threadCount = 10;
        UUID hubId = UUID.randomUUID();

        deliveryManagerRepository.saveAndFlush(
            DeliveryManager.create(
                UUID.randomUUID(),
                hubId,
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

        ConcurrentLinkedQueue<Throwable> exceptions = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    CreateDeliveryManagerRequest request = new CreateDeliveryManagerRequest(
                        UUID.randomUUID(),
                        hubId,
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

        List<DeliveryManager> result = deliveryManagerRepository.findAll().stream()
            .filter(manager -> manager.getType() == DeliveryManagerType.HUB_DELIVERY_MANAGER)
            .filter(manager -> hubId.equals(manager.getHubId()))
            .toList();

        assertThat(result).isNotEmpty();

        Set<Integer> sequences = result.stream()
            .map(DeliveryManager::getDeliverySequence)
            .collect(Collectors.toSet());

        assertThat(sequences).hasSize(result.size());
        assertThat(result)
            .allMatch(manager -> manager.getType() == DeliveryManagerType.HUB_DELIVERY_MANAGER)
            .allMatch(manager -> hubId.equals(manager.getHubId()));
        assertThat(sequences).contains(0);
        assertThat(result.size()).isGreaterThanOrEqualTo(1);
    }
}
