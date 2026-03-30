package com.team.authservice.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum AuthErrorCode implements ErrorCode {
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
