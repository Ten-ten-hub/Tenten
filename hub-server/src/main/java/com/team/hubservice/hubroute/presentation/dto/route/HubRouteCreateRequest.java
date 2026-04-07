package com.team.hubservice.hubroute.presentation.dto.route;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

public record HubRouteCreateRequest(
    @NotNull(message = "출발 허브 ID는 필수입니다.") UUID departureHubId,
    @NotNull(message = "도착 허브 ID는 필수입니다.") UUID arrivalHubId,
    @NotNull(message = "소요 시간은 필수입니다.") @PositiveOrZero Integer duration,
    @NotNull(message = "이동 거리는 필수입니다.") @PositiveOrZero Double distance
) {}
