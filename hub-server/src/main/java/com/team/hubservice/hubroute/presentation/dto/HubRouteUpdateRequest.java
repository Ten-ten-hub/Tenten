package com.team.hubservice.hubroute.presentation.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record HubRouteUpdateRequest(
    @PositiveOrZero Integer duration,
    @PositiveOrZero Double distance
) {}
