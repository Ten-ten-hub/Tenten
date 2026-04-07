package com.team.hubservice.hubroute.presentation.dto.route;

import jakarta.validation.constraints.PositiveOrZero;

public record HubRouteUpdateRequest(
    @PositiveOrZero Integer duration,
    @PositiveOrZero Double distance
) {}
