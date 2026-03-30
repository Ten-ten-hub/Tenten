package com.team.deliveryservice.presentation.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleServiceException(ServiceException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.error("Unexpected error occurred", e);
        return ResponseEntity.status(errorCode.status())
            .body(ApiResponse.fail(errorCode.code(), errorCode.message()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.fail(
                ErrorCode.COMMON_INVALID_INPUT.code(),
                ErrorCode.COMMON_INVALID_INPUT.message()
            ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.fail(
                ErrorCode.COMMON_INVALID_INPUT.code(),
                ErrorCode.COMMON_INVALID_INPUT.message()
            ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        return ResponseEntity.status(ErrorCode.COMMON_INTERNAL_ERROR.status())
            .body(ApiResponse.fail(
                ErrorCode.COMMON_INTERNAL_ERROR.code(),
                ErrorCode.COMMON_INTERNAL_ERROR.message()
            ));
    }
}
