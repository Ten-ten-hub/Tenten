package com.team.userservice.user.users.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginReqDto(
    @NotBlank String loginId,
    @NotBlank String password
) {

}
