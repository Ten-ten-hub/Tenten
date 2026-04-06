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

    public HubRouteAiService(HubRouteRepository hubRouteRepository) {
        this.hubRouteRepository = hubRouteRepository;
    }

    public AiRouteResponse getRouteInfoForAi(UUID originId, UUID destinationId) {
        return hubRouteRepository.findRouteWithCoordinates(originId, destinationId)
            .orElseThrow(() -> new BusinessException(HubRouteErrorCode.ROUTE_NOT_FOUND));

    }
}
