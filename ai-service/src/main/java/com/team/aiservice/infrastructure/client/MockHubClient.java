package com.team.aiservice.infrastructure.client;

import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
public class MockHubClient implements HubClient {
    @Override
    public HubRouteResponse getRoute(UUID originId, UUID destinationId) {
        // AI가 분석할 수 있도록 실제 서울과 부산의 위경도 더미 데이터를 반환
        return new HubRouteResponse(
            120,      // 소요시간 (분)
            325.5,    // 거리 (km)
            37.4742,  // 출발지(서울) 위도
            127.1236, // 출발지(서울) 경도
            35.1796,  // 도착지(부산) 위도
            129.0756  // 도착지(부산) 경도
        );
    }
}
