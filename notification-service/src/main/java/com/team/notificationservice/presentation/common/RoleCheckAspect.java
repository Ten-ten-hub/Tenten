package com.team.notificationservice.presentation.common;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
public class RoleCheckAspect {

    @Before("@annotation(requireRole)")
    public void checkRole(RequireRole requireRole) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        // 게이트웨이가 JWT를 파싱해 넣어준 헤더를 읽음
        String userRole = request.getHeader("X-User-Role");
        if (userRole == null) {
            throw new ServiceException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        String normalizedUserRole = userRole.trim().toUpperCase();
        boolean hasRole = Arrays.stream(requireRole.value())
            .map(String::toUpperCase)
            .anyMatch(role -> role.equals(normalizedUserRole));

        if (!hasRole) {
            log.warn("권한 부족: 필요 {}, 입력 {}", Arrays.toString(requireRole.value()), userRole);
            throw new ServiceException(ErrorCode.AUTH_FORBIDDEN);
        }
    }
}
