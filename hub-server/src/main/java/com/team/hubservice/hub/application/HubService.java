package com.team.hubservice.hub.application;

import com.team.hubservice.hub.domain.Hub;
import com.team.hubservice.hub.dto.HubCreateRequest;
import com.team.hubservice.hub.dto.HubResponse;
import com.team.hubservice.hub.dto.HubUpdateRequest;
import com.team.hubservice.hub.repository.HubRepository;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class HubService {

    private final HubRepository hubRepository;

    public HubService(HubRepository hubRepository) {
        this.hubRepository = hubRepository;
    }

    @Transactional
    public HubResponse createHub(HubCreateRequest request) {
        if (hubRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("이미 동일한 이름의 허브가 존재합니다.");
        }

        Hub hub = Hub.create(
                request.name(),
                request.address(),
                request.latitude(),
                request.longitude()
        );

        return HubResponse.from(hubRepository.save(hub));
    }

    public Page<HubResponse> getHubs(String name, Pageable pageable) {
        if (name != null && !name.isBlank()) {
            return hubRepository.findByNameContaining(name, pageable).map(HubResponse::from);
        }
        return hubRepository.findAll(pageable).map(HubResponse::from);
    }

    @Cacheable(value = "hubs", key = "#hubId")
    public HubResponse getHub(UUID hubId) {
        return HubResponse.from(findHubById(hubId));
    }

    @Transactional
    @CacheEvict(value = "hubs", key = "#hubId")
    public HubResponse updateHub(UUID hubId, HubUpdateRequest request) {
        Hub hub = findHubById(hubId);
        hub.update(request.name(), request.latitude(), request.longitude());
        return HubResponse.from(hub);
    }

    @Transactional
    @CacheEvict(value = "hubs", key = "#hubId")
    public void deleteHub(UUID hubId, UUID deletedBy) {
        Hub hub = findHubById(hubId);
        checkActiveRoutesAndCompanies(hubId);
        hub.softDelete(deletedBy);
    }

    private Hub findHubById(UUID hubId) {
        return hubRepository.findById(hubId)
                .orElseThrow(() -> new IllegalArgumentException("요청한 허브 정보를 찾을 수 없습니다."));
    }

    private void checkActiveRoutesAndCompanies(UUID hubId) {
        // TODO: 연관 데이터 검증 로직 구현 예정 구역
    }

    public boolean checkHubExists(UUID hubId) {
        return hubRepository.existsById(hubId);
    }
}
