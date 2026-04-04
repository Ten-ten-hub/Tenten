package com.team.authservice.global.exception;

import com.team.authservice.global.error.AuthErrorCode;
import com.team.common.exception.BusinessException;
import lombok.Getter;

@Getter
public class AuthException extends BusinessException {
    public AuthException(AuthErrorCode errorCode) {
        super(errorCode);
    }
}
