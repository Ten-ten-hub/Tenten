package com.team.userservice.global.auth;

import com.team.userservice.global.domain.error.UserErrorCode;
import com.team.userservice.global.exception.UserException;
import com.team.userservice.user.core.enums.Role;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class RoleCheckAspect {

    private final HttpServletRequest request;

    @Before("@annotation(requireRole)")
    public void checkRole(RequireRole requireRole) {
        String roleHeader = request.getHeader("X-User-Role");

        Role requestRole;
        try {
            requestRole = Role.valueOf(roleHeader);
        } catch (Exception e) {
            throw new UserException(UserErrorCode.FORBIDDEN);
        }

        boolean hasPermission = Arrays.asList(requireRole.value()).contains(requestRole);
        if (!hasPermission) {
            throw new UserException(UserErrorCode.FORBIDDEN);
        }
    }
}
