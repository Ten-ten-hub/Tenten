package com.team.userservice.user.users.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginReq(
    @NotBlank String loginId,
    @NotBlank String password
) {

}
