package com.team.hubservice.hub.application;

import com.team.common.exception.BusinessException;
import com.team.hubservice.global.exception.HubErrorCode;
import com.team.hubservice.hub.domain.Hub;
import com.team.hubservice.hub.domain.HubRepository;
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
    public HubResult createHub(HubCreateCommand command) {
        if (hubRepository.existsByName(command.name())) {
            throw new BusinessException(HubErrorCode.HUB_ALREADY_EXISTS);
        }

        Hub hub = Hub.create(
            command.name(),
            command.address(),
            command.latitude(),
            command.longitude()
        );

        return HubResult.from(hubRepository.save(hub));
    }

    public Page<HubResult> getHubs(String name, Pageable pageable) {
        if (name != null && !name.isBlank()) {
            return hubRepository.findByNameContaining(name, pageable).map(HubResult::from);
        }
        return hubRepository.findAll(pageable).map(HubResult::from);
    }

    @Cacheable(value = "hubs", key = "#hubId")
    public HubResult getHub(UUID hubId) {
        return HubResult.from(findHubById(hubId));
    }

    public HubResult getHubInternal(UUID hubId) {
        return HubResult.from(findHubById(hubId));
    }

    @Transactional
    @CacheEvict(value = "hubs", key = "#hubId")
    public HubResult updateHub(UUID hubId, HubUpdateCommand command) {
        Hub hub = findHubById(hubId);
        hub.update(command.name(), command.latitude(), command.longitude());
        return HubResult.from(hub);
    }

    @Transactional
    @CacheEvict(value = "hubs", key = "#hubId")
    public void deleteHub(UUID hubId, UUID deletedBy) {
        Hub hub = findHubById(hubId);
        checkActiveRoutesAndCompanies(hubId);
        hub.softDelete(deletedBy);
    }

    public boolean checkHubExists(UUID hubId) {
        return hubRepository.existsById(hubId);
    }

    private Hub findHubById(UUID hubId) {
        return hubRepository.findById(hubId)
            .orElseThrow(() -> new BusinessException(HubErrorCode.HUB_NOT_FOUND));
    }

    private void checkActiveRoutesAndCompanies(UUID hubId) {
        // TODO: 연관 데이터 검증 로직 구현 예정 구역
    }
}
