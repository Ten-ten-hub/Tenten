package com.team.hubservice.hubroute.application;

import com.team.common.exception.BusinessException;
import com.team.hubservice.global.exception.HubErrorCode;
import com.team.hubservice.global.exception.HubRouteErrorCode;
import com.team.hubservice.hub.domain.Hub;
import com.team.hubservice.hub.domain.HubRepository;
import com.team.hubservice.hubroute.domain.HubRoute;
import com.team.hubservice.hubroute.domain.HubRouteRepository;
import com.team.hubservice.hubroute.presentation.dto.AiRouteResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class HubRouteAiService {

    private final HubRouteRepository hubRouteRepository;
    private final HubRepository hubRepository;

    public HubRouteAiService(HubRouteRepository hubRouteRepository, HubRepository hubRepository) {
        this.hubRouteRepository = hubRouteRepository;
        this.hubRepository = hubRepository;
    }

    public AiRouteResponse getRouteInfoForAi(UUID originId, UUID destinationId) {
        HubRoute route = hubRouteRepository.findByDepartureHubIdAndArrivalHubId(originId, destinationId)
            .orElseThrow(() -> new BusinessException(HubRouteErrorCode.ROUTE_NOT_FOUND));

        Hub originHub = hubRepository.findById(originId)
            .orElseThrow(() -> new BusinessException(HubErrorCode.HUB_NOT_FOUND));

        Hub destHub = hubRepository.findById(destinationId)
            .orElseThrow(() -> new BusinessException(HubErrorCode.HUB_NOT_FOUND));

        return new AiRouteResponse(
            route.getDuration(),
            route.getDistance(),
            originHub.getLatitude(),
            originHub.getLongitude(),
            destHub.getLatitude(),
            destHub.getLongitude()
        );
    }
}
