package com.team.userservice.global.config;

import java.util.List;
import java.util.Set;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PageableConfig implements WebMvcConfigurer {

    @Override // 커스텀 리졸버 등록
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new PageableHandlerMethodArgumentResolver() {
            @Override
            // HTTP 요청에서 Pageable 파라미터를 어떻게 만들어낼지 정의하는 메서드
            public Pageable resolveArgument(MethodParameter methodParameter, ModelAndViewContainer mavContainer,
                                            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                Pageable pageable = super.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory);
                Set<Integer> allowedSizes = Set.of(10, 30, 50);

                int size = allowedSizes.contains(pageable.getPageSize()) ? pageable.getPageSize() : 10;

                return PageRequest.of(pageable.getPageNumber(), size, pageable.getSort());
            }
        });
    }
}
