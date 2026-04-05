package com.team.companyservice.global.auth;

import com.team.companyservice.global.error.CompanyErrorCode;
import com.team.companyservice.global.error.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

// @RequireRole 이 붙은 엔드포인트의 접근 권한을 검사하는 AOP
@Aspect
@Component
@RequiredArgsConstructor
public class RoleCheckAspect {

    private final HttpServletRequest request;

    @Before("@annotation(requireRole)")
    public void checkRole(RequireRole requireRole) {
        String roleHeader = request.getHeader("X-User-Role");

        // 역할 헤더가 없으면 인증 실패로 처리
        if (roleHeader == null || roleHeader.isBlank()) {
            throw new ServiceException(CompanyErrorCode.COMMON_UNAUTHORIZED);
        }

        String normalizedRole = roleHeader.trim().toUpperCase();

        boolean hasPermission = Arrays.stream(requireRole.value())
            .map(String::trim)
            .map(String::toUpperCase)
            .anyMatch(allowedRole -> allowedRole.equals(normalizedRole));

        // 허용된 역할이 아니면 접근 거부
        if (!hasPermission) {
            throw new ServiceException(CompanyErrorCode.COMMON_ACCESS_DENIED);
        }
    }
}
