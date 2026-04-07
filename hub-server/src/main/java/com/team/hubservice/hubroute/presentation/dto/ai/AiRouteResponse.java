package com.team.hubservice.hubroute.presentation.dto.ai;

public record AiRouteResponse(
    Integer duration,
    Double distance,
    Double originLat,
    Double originLng,
    Double destLat,
    Double destLng
) {
}
