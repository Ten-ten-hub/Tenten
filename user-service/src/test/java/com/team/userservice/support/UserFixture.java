package com.team.userservice.support;

import com.team.userservice.user.core.User;
import com.team.userservice.user.core.enums.AffiliatedStatus;
import com.team.userservice.user.core.enums.Role;
import com.team.userservice.user.core.enums.SignupStatus;
import com.team.userservice.user.users.application.dto.LoginServiceDto;
import com.team.userservice.user.users.application.dto.SignUpResultDto;
import com.team.userservice.user.users.application.dto.SignUpServiceDto;
import com.team.userservice.user.users.application.dto.UserDataDto;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserFixture {

    public static final UUID TEST_USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    public static final UUID TEST_MASTER_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");
    public static final UUID TEST_HUB_ID = UUID.fromString("770e8400-e29b-41d4-a716-446655440002");

    public static final String TEST_LOGIN_ID = "testuser1";
    public static final String TEST_NAME = "홍길동";
    public static final String TEST_EMAIL = "test@example.com";
    public static final String TEST_PHONE = "010-1234-5678";
    public static final String TEST_SLACK_ID = "U12345678";
    public static final String TEST_PASSWORD = "Test1234!@";

    private static final LocalDateTime FIXED_CREATED_AT = LocalDateTime.of(2024, 1, 1, 0, 0);
    private static final LocalDateTime FIXED_UPDATED_AT = LocalDateTime.of(2024, 1, 2, 12, 0);
    private static final LocalDateTime FIXED_LOGIN_AT = LocalDateTime.of(2024, 1, 3, 9, 0);

    public static User createUser() {
        User user = User.create()
                .loginId(TEST_LOGIN_ID)
                .password("encodedPassword")
                .name(TEST_NAME)
                .slackId(TEST_SLACK_ID)
                .email(TEST_EMAIL)
                .phoneNumber(TEST_PHONE)
                .build();
        ReflectionTestUtils.setField(user, "id", TEST_USER_ID);
        ReflectionTestUtils.setField(user, "createdAt", FIXED_CREATED_AT);
        ReflectionTestUtils.setField(user, "updatedAt", FIXED_UPDATED_AT);
        return user;
    }

    public static SignUpResultDto createSignUpResultDto() {
        return new SignUpResultDto(
                TEST_USER_ID,
                TEST_LOGIN_ID,
                TEST_NAME,
                Role.NONE,
                TEST_EMAIL,
                SignupStatus.PENDING,
                FIXED_CREATED_AT
        );
    }

    public static SignUpServiceDto createSignUpServiceDto() {
        return new SignUpServiceDto(TEST_NAME, TEST_LOGIN_ID, TEST_PASSWORD, TEST_SLACK_ID, TEST_EMAIL, TEST_PHONE);
    }

    public static LoginServiceDto createLoginServiceDto() {
        return LoginServiceDto.from(TEST_USER_ID, Role.HUB_ADMIN);
    }

    public static UserDataDto createUserDataDto() {
        return new UserDataDto(
                TEST_USER_ID,
                Role.HUB_ADMIN,
                SignupStatus.APPROVED,
                TEST_NAME,
                TEST_LOGIN_ID,
                TEST_EMAIL,
                TEST_PHONE,
                TEST_SLACK_ID,
                AffiliatedStatus.UNAFFILIATED,
                null,
                FIXED_LOGIN_AT,
                FIXED_CREATED_AT,
                FIXED_UPDATED_AT
        );
    }
}