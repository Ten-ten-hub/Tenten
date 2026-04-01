package com.team.deliveryservice.global.config;

import com.team.deliveryservice.global.common.CurrentUser;
import com.team.deliveryservice.global.error.DeliveryErrorCode;
import com.team.deliveryservice.global.error.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
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
        @Nullable ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest,
        @Nullable WebDataBinderFactory binderFactory
    ) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new ServiceException(DeliveryErrorCode.COMMON_INTERNAL_ERROR);
        }

        return new CurrentUser(
            parseUuid(request.getHeader("X-User-Id")),
            request.getHeader("X-Role"),
            parseUuid(request.getHeader("X-Hub-Id")),
            parseUuid(request.getHeader("X-Company-Id"))
        );
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new ServiceException(DeliveryErrorCode.COMMON_INVALID_INPUT);
        }
    }
}
