package com.team.hubservice.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret) {
    public JwtProperties {
        Assert.hasText(secret, "JWT secret 값이 설정되지 않았습니다. application.properties를 확인해주세요.");
    }
}
