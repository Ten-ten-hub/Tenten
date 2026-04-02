package com.team.deliveryservice.global.error;

import com.team.common.exception.BusinessException;

public class ServiceException extends BusinessException {
    public ServiceException(DeliveryErrorCode errorCode) {
        super(errorCode);
    }
}
