package com.team.companyservice.presentation.common;

import java.util.UUID;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;

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
            org.springframework.web.bind.support.WebDataBinderFactory binderFactory
    ) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);

        String userId = request.getHeader("X-User-Id");
        String role = request.getHeader("X-Role");
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
