package com.team.notificationservice.presentation.common;

import com.team.common.ApiResponse;
import com.team.common.exception.BusinessException;
import lombok.Getter;

import java.util.List;

public class ServiceException extends BusinessException {
    private final ErrorCode errorCode;
    // 상세 에러 정보를 담을 리스트 추가
    @Getter
    private final List<ApiResponse.ValidationError> errors;

    // 기존 생성자 (상세 에러가 없는 경우)
    public ServiceException(ErrorCode errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
        this.errors = null;
    }

    // 새로운 생성자 (상세 에러가 있는 경우)
    public ServiceException(ErrorCode errorCode, List<ApiResponse.ValidationError> errors) {
        super(errorCode);
        this.errorCode = errorCode;
        this.errors = errors;
    }

    // 원인 보존을 위한 생성자 추가
    public ServiceException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
        this.errorCode = errorCode;
        this.errors = null;
    }

    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }

}
