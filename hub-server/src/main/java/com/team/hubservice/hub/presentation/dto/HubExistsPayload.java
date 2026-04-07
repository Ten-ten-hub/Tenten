package com.team.hubservice.hub.presentation.dto;

import java.util.UUID;

public record HubExistsPayload(UUID hubId, boolean exists) {

}
