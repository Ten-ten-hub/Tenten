package com.team.userservice.global.exception;

import com.team.common.exception.BusinessException;
import com.team.userservice.global.domain.error.UserErrorCode;
import lombok.Getter;

@Getter
public class UserException extends BusinessException {
    public UserException(UserErrorCode errorCode) {
        super(errorCode);
    }
}


