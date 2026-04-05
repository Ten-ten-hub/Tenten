package com.team.companyservice.global.config;

import com.team.companyservice.global.common.CurrentUser;
import com.team.companyservice.global.error.CompanyErrorCode;
import com.team.companyservice.global.error.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

// 요청 헤더에서 사용자 정보를 읽어 CurrentUser로 변환
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(CurrentUser.class);
    }

    @Override
    public Object resolveArgument(
        MethodParameter parameter,
        @Nullable ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest,
        @Nullable WebDataBinderFactory binderFactory
    ) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new ServiceException(CompanyErrorCode.COMMON_INTERNAL_ERROR);
        }

        String userIdHeader = request.getHeader("X-User-Id");
        String roleHeader = request.getHeader("X-User-Role");
        String hubIdHeader = request.getHeader("X-Hub-Id");
        String companyIdHeader = request.getHeader("X-Company-Id");

        // 외부 API는 최소 사용자 ID와 역할 정보가 있어야 함
        if (userIdHeader == null || userIdHeader.isBlank()
            || roleHeader == null || roleHeader.isBlank()) {
            throw new ServiceException(CompanyErrorCode.COMMON_UNAUTHORIZED);
        }

        return new CurrentUser(
            parseUuid(userIdHeader),
            roleHeader,
            parseUuid(hubIdHeader),
            parseUuid(companyIdHeader)
        );
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new ServiceException(CompanyErrorCode.COMMON_INVALID_INPUT);
        }
    }
}
