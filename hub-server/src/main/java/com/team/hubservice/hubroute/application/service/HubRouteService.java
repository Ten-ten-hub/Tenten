package com.team.hubservice.hubroute.application.service;

import com.team.common.exception.BusinessException;
import com.team.hubservice.hubroute.application.dto.route.HubRouteCreateCommand;
import com.team.hubservice.hubroute.application.dto.route.HubRouteResult;
import com.team.hubservice.hubroute.application.dto.route.HubRouteUpdateCommand;
import com.team.hubservice.hubroute.domain.HubRoute;
import com.team.hubservice.hubroute.domain.HubRouteRepository;
import com.team.hubservice.global.exception.HubRouteErrorCode;
import java.util.UUID;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class HubRouteService {

    private final HubRouteRepository hubRouteRepository;

    public HubRouteService(HubRouteRepository hubRouteRepository) {
        this.hubRouteRepository = hubRouteRepository;
    }

    @Transactional
    @CacheEvict(value = "optimalRoutes", allEntries = true)
    public HubRouteResult createHubRoute(HubRouteCreateCommand command) {
        if (hubRouteRepository.existsByDepartureHubIdAndArrivalHubId(command.departureHubId(), command.arrivalHubId())) {
            throw new BusinessException(HubRouteErrorCode.ROUTE_DUPLICATED);
        }

        HubRoute route = HubRoute.create(
            command.departureHubId(),
            command.arrivalHubId(),
            command.duration(),
            command.distance()
        );

        try {
            return HubRouteResult.from(hubRouteRepository.save(route));
        } catch (DataIntegrityViolationException e) {
            // 동시성 문제로 DB 유니크 인덱스에 걸렸을 때 처리
            throw new BusinessException(HubRouteErrorCode.ROUTE_DUPLICATED);
        }
    }

    @Cacheable(value = "hubRoutes", key = "#routeId")
    public HubRouteResult getHubRoute(UUID routeId) {
        return HubRouteResult.from(findRouteById(routeId));
    }

    public Page<HubRouteResult> getHubRoutes(UUID departureHubId, Pageable pageable) {
        if (departureHubId != null) {
            return hubRouteRepository.findByDepartureHubId(departureHubId, pageable).map(HubRouteResult::from);
        }
        return hubRouteRepository.findAll(pageable).map(HubRouteResult::from);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "hubRoutes", key = "#routeId"),
        @CacheEvict(value = "optimalRoutes", allEntries = true)
    })
    public HubRouteResult updateHubRoute(UUID routeId, HubRouteUpdateCommand command) {
        HubRoute route = findRouteById(routeId);
        route.update(command.duration(), command.distance());
        return HubRouteResult.from(route);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "hubRoutes", key = "#routeId"),
        @CacheEvict(value = "optimalRoutes", allEntries = true)
    })
    public void deleteHubRoute(UUID routeId, UUID deletedBy) {
        HubRoute route = findRouteById(routeId);
        route.softDelete(deletedBy);
    }

    private HubRoute findRouteById(UUID routeId) {
        return hubRouteRepository.findById(routeId)
            .orElseThrow(() -> new BusinessException(HubRouteErrorCode.ROUTE_NOT_FOUND));
    }
}
