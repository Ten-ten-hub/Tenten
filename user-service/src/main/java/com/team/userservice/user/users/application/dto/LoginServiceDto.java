package com.team.userservice.user.users.application.dto;

import com.team.userservice.user.core.User;
import com.team.userservice.user.core.enums.Role;

public record LoginServiceDto(
        String loginId,
        String password,
        Role role
) {
    public static LoginServiceDto from(User userfromLoginId) {
        return new LoginServiceDto(
                userfromLoginId.getLoginId(),
                userfromLoginId.getPassword(),
                userfromLoginId.getRole()
        );
    }
}
