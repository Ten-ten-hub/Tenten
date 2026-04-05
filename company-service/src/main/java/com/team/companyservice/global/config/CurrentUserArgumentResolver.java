package com.team.companyservice.global.config;

import java.util.UUID;

import com.team.companyservice.global.common.CurrentUser;
import com.team.companyservice.global.error.CompanyErrorCode;
import com.team.companyservice.global.error.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(CurrentUser.class);
    }

    @Override
    public Object resolveArgument(
        MethodParameter parameter,
        ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest,
        WebDataBinderFactory binderFactory
    ) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);

        // 게이트웨이/인증 필터에서 전달한 사용자 헤더를 읽는다.
        String userId = request.getHeader("X-User-Id");
        String role = request.getHeader("X-User-Role");
        String hubId = request.getHeader("X-Hub-Id");
        String companyId = request.getHeader("X-Company-Id");

        if (userId == null || role == null) {
            throw new ServiceException(CompanyErrorCode.COMMON_UNAUTHORIZED);
        }

        return new CurrentUser(
            UUID.fromString(userId),
            role,
            hubId != null && !hubId.isBlank() ? UUID.fromString(hubId) : null,
            companyId != null && !companyId.isBlank() ? UUID.fromString(companyId) : null
        );
    }
}
