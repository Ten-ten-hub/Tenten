package com.team.notificationservice.presentation.common;

import java.util.List;

public class ServiceException extends RuntimeException {
    private final ErrorCode errorCode;
    // 상세 에러 정보를 담을 리스트 추가
    private final List<ApiResponse.ValidationError> errors;

    // 기존 생성자 (상세 에러가 없는 경우)
    public ServiceException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.errors = null;
    }

    // 새로운 생성자 (상세 에러가 있는 경우)
    public ServiceException(ErrorCode errorCode, List<ApiResponse.ValidationError> errors) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.errors = errors;
    }

    // 원인 보존을 위한 생성자 추가
    public ServiceException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.errors = null;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public List<ApiResponse.ValidationError> getErrors() {
        return errors;
    }
}
