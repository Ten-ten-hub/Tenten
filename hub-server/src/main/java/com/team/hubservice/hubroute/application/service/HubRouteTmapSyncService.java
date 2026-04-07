package com.team.hubservice.hubroute.application.service;

import com.team.common.exception.BusinessException;
import com.team.hubservice.global.exception.HubErrorCode;
import com.team.hubservice.hub.domain.Hub;
import com.team.hubservice.hub.domain.HubRepository;
import com.team.hubservice.hubroute.application.dto.sync.TmapHubRouteBulkSyncResult;
import com.team.hubservice.hubroute.application.dto.sync.TmapHubRouteSyncResult;
import com.team.hubservice.hubroute.infrastructure.tmap.TmapProperties;
import com.team.hubservice.hubroute.infrastructure.tmap.TmapRouteClient;
import com.team.hubservice.hubroute.infrastructure.tmap.TmapRouteMetrics;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class HubRouteTmapSyncService {

    private final HubRepository hubRepository;
    private final TmapRouteClient tmapRouteClient;
    private final HubRouteTmapPairWriter pairWriter;
    private final TmapProperties tmapProperties;

    public HubRouteTmapSyncService(
        HubRepository hubRepository,
        TmapRouteClient tmapRouteClient,
        HubRouteTmapPairWriter pairWriter,
        TmapProperties tmapProperties
    ) {
        this.hubRepository = hubRepository;
        this.tmapRouteClient = tmapRouteClient;
        this.pairWriter = pairWriter;
        this.tmapProperties = tmapProperties;
    }

    public TmapHubRouteSyncResult syncOnePair(UUID departureHubId, UUID arrivalHubId) {
        Hub departure = hubRepository.findById(departureHubId)
            .orElseThrow(() -> new BusinessException(HubErrorCode.HUB_NOT_FOUND));
        Hub arrival = hubRepository.findById(arrivalHubId)
            .orElseThrow(() -> new BusinessException(HubErrorCode.HUB_NOT_FOUND));

        TmapRouteMetrics metrics = tmapRouteClient.fetchDrivingMetrics(
            departure.getLongitude(),
            departure.getLatitude(),
            arrival.getLongitude(),
            arrival.getLatitude()
        );

        return pairWriter.upsert(departureHubId, arrivalHubId, metrics);
    }

    public TmapHubRouteBulkSyncResult syncAllDirectedPairs() {
        List<Hub> hubs = hubRepository.findAll();
        int attempted = 0;
        int succeeded = 0;
        List<String> errors = new ArrayList<>();

        for (Hub from : hubs) {
            for (Hub to : hubs) {
                if (from.getId().equals(to.getId())) {
                    continue;
                }
                attempted++;
                try {
                    syncOnePair(from.getId(), to.getId());
                    succeeded++;
                } catch (Exception e) {
                    String extra = "";
                    errors.add(from.getId() + " -> " + to.getId() + ": " + e.getMessage() + extra);
                }
                sleepIfConfigured();
            }
        }
        return new TmapHubRouteBulkSyncResult(attempted, succeeded, attempted - succeeded, errors);
    }

    private void sleepIfConfigured() {
        long delay = tmapProperties.getRequestDelayMs();
        if (delay <= 0) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
