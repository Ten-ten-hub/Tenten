package com.team.aiservice.infrastructure.client;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "hub-service", contextId = "actualHubClient", primary = false) //TODO primary=false 는 mock 제거 후 삭제
public interface HubClient {

    @GetMapping("/api/v1/hubs/routes")
    HubRouteResponse getRoute(@RequestParam UUID originId,
                              @RequestParam UUID destinationId);

    record HubRouteResponse(
        Integer duration,
        Double distance,
        Double originLat,
        Double originLng,
        Double destLat,
        Double destLng
    ) {
    }
}
