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

        if (userRole == null || userRole.isBlank()) {
            log.warn("권한 체크 실패: 헤더에 X-User-Role이 없음");
            throw new ServiceException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        boolean hasRole = Arrays.asList(requireRole.value()).contains(userRole);

        if (!hasRole) {
            log.warn("권한 부족: 필요 권한 {}, 유저 권한 {}", Arrays.toString(requireRole.value()), userRole);
            throw new ServiceException(ErrorCode.AUTH_INVALID_TOKEN);
        }
    }
}
