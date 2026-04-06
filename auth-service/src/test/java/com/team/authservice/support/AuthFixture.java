package com.team.authservice.support;

import com.team.authservice.auth.application.dto.TokenDto;
import com.team.authservice.auth.infrastructure.feign.dto.UserVerifyRes;
import com.team.authservice.core.enums.Role;

import java.util.UUID;

public class AuthFixture {

    public static final UUID TEST_USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    public static final String TEST_LOGIN_ID = "testuser1";
    public static final String TEST_PASSWORD = "Test1234!@";
    public static final String ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiJ9.access.token";
    public static final String REFRESH_TOKEN = "eyJhbGciOiJIUzI1NiJ9.refresh.token";
    public static final String NEW_ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiJ9.new.access.token";
    public static final String NEW_REFRESH_TOKEN = "eyJhbGciOiJIUzI1NiJ9.new.refresh.token";

    public static TokenDto createTokenDto() {
        return new TokenDto(ACCESS_TOKEN, REFRESH_TOKEN);
    }

    public static TokenDto createNewTokenDto() {
        return new TokenDto(NEW_ACCESS_TOKEN, NEW_REFRESH_TOKEN);
    }

    public static UserVerifyRes createUserVerifyRes() {
        return new UserVerifyRes(TEST_USER_ID, Role.HUB_ADMIN);
    }
}