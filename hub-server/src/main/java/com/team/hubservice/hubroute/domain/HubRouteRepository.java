package com.team.hubservice.hubroute.domain;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HubRouteRepository extends JpaRepository<HubRoute, UUID> {
    // 중복 경로 검증용
    boolean existsByDepartureHubIdAndArrivalHubId(UUID departureHubId, UUID arrivalHubId);

    // 조건 검색용 (출발 허브 기준)
    Page<HubRoute> findByDepartureHubId(UUID departureHubId, Pageable pageable);
}
