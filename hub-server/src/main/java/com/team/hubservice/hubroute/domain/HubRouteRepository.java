package com.team.hubservice.hubroute.domain;

import com.team.hubservice.hubroute.presentation.dto.AiRouteResponse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HubRouteRepository extends JpaRepository<HubRoute, UUID> {
    // 중복 경로 검증용
    boolean existsByDepartureHubIdAndArrivalHubId(UUID departureHubId, UUID arrivalHubId);

    // 조건 검색용 (출발 허브 기준)
    Page<HubRoute> findByDepartureHubId(UUID departureHubId, Pageable pageable);

    // AI 경로 조회용
    @Query("""
        SELECT new com.team.hubservice.hubroute.presentation.dto.AiRouteResponse(
            r.duration, r.distance,
            origin.latitude, origin.longitude,
            dest.latitude, dest.longitude
        )
        FROM HubRoute r
        JOIN Hub origin ON origin.id = r.departureHubId
        JOIN Hub dest   ON dest.id   = r.arrivalHubId
        WHERE r.departureHubId = :originId
          AND r.arrivalHubId   = :destinationId
    """)
    Optional<AiRouteResponse> findRouteWithCoordinates(
        @Param("originId") UUID originId,
        @Param("destinationId") UUID destinationId
    );
}
