package com.team.userservice.global.exception;

import com.team.userservice.global.domain.error.UserErrorCode;
import lombok.Getter;

@Getter
public class UserException extends RuntimeException {
    private final UserErrorCode errorCode;

    public UserException(UserErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

}
