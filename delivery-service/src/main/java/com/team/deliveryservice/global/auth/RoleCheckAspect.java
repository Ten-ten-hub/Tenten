package com.team.deliveryservice.global.auth;

import com.team.deliveryservice.global.common.CurrentUser;
import com.team.deliveryservice.global.error.DeliveryErrorCode;
import com.team.deliveryservice.global.error.ServiceException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RoleCheckAspect {

    @Before("@annotation(requireRole)")
    public void checkRole(JoinPoint joinPoint, RequireRole requireRole) {
        CurrentUser currentUser = extractCurrentUser(joinPoint);
        if (currentUser == null || currentUser.role() == null || currentUser.role().isBlank()) {
            throw new ServiceException(DeliveryErrorCode.COMMON_UNAUTHORIZED);
        }

        Set<String> allowedRoles = Arrays.stream(requireRole.value())
            .map(String::toUpperCase)
            .collect(Collectors.toSet());

        String userRole = currentUser.role().toUpperCase();

        if (!allowedRoles.contains(userRole)) {
            throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
        }
    }

    private CurrentUser extractCurrentUser(JoinPoint joinPoint) {
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof CurrentUser currentUser) {
                return currentUser;
            }
        }
        return null;
    }
}
