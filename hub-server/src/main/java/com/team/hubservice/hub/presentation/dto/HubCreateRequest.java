package com.team.hubservice.hub.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HubCreateRequest(
        @NotBlank(message = "허브 이름은 필수입니다.")
        String name,

        @NotBlank(message = "허브 주소는 필수입니다.")
        String address,

        @NotNull(message = "위도 값은 필수입니다.")
        Double latitude,

        @NotNull(message = "경도 값은 필수입니다.")
        Double longitude
) {
}
