package com.team.userservice.user.users.presentation.dto.request;

import com.team.userservice.user.core.enums.Role;
import jakarta.validation.constraints.NotNull;

public record RegisterReqDto(
    @NotNull(message = "부여할 역할을 입력해주세요.")
    Role giveRole
) {
}
