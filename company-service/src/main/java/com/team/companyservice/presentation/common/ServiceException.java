package com.team.companyservice.presentation.common;

import com.team.common.exception.BusinessException;

public class ServiceException extends BusinessException {

    public ServiceException(ErrorCode errorCode) {
        super(errorCode);
    }
}
