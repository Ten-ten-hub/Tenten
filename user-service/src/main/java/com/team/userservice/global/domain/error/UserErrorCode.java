package com.team.userservice.global.domain.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum UserErrorCode implements ErrorCode {

    FORBIDDEN(HttpStatus.FORBIDDEN, "USER_403", "접근 권한이 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404", "존재하지 않는 유저입니다."),
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "USER_409_1", "이미 사용 중인 아이디입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER_409_2", "이미 사용 중인 이메일입니다."),
    ALREADY_REGISTERED_USER(HttpStatus.CONFLICT, "USER_409_3", "이미 등록이 완료된 유저입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "USER_401", "아이디 또는 비밀번호가 일치하지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
