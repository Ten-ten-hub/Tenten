package com.team.notificationservice.presentation.common;

import com.team.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class NotificationExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleServiceException(ServiceException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.error("ServiceException: {}", errorCode.getMessage());
        return ResponseEntity
            .status(errorCode.getStatus())
            .body(ApiResponse.error(
                errorCode.getCode(),
                errorCode.getMessage(),
                e.getErrors()
            ));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidationException(BindException e) {
        // 보안을 위해 원본 메시지(e.getMessage()) 대신 필드명과 에러 개수만 로깅
        String fields = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getField)
            .collect(Collectors.joining(", "));
        log.warn("ValidationException: {} field error(s) in [{}]", e.getBindingResult().getFieldErrorCount(), fields);

        List<ApiResponse.ValidationError> errors = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new ApiResponse.ValidationError(
                error.getField(),
                maskSensitiveInfo(error.getField(), String.valueOf(error.getRejectedValue())),
                error.getDefaultMessage()))
            .toList();

        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error(
                ErrorCode.COMMON_INVALID_INPUT_VALUE.getCode(),
                ErrorCode.COMMON_INVALID_INPUT_VALUE.getMessage(),
                errors
            ));
    }

    // 민감 정보 마스킹 로직 (예: 이메일)
    private String maskSensitiveInfo(String field, String value) {
        if (value == null || value.equals("null")) {
            return value;
        }
        if (field.toLowerCase().contains("email") && value.contains("@")) {
            return value.replaceAll("(^[^@]{3}|(?!^)\\G)[^@]", "$1*");
        }
        return value;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled Exception: ", e); // 모든 예외 로깅
        return ResponseEntity
            .status(ErrorCode.COMMON_INTERNAL_SERVER_ERROR.getStatus())
            .body(ApiResponse.error(
                ErrorCode.COMMON_INTERNAL_SERVER_ERROR.getCode(),
                ErrorCode.COMMON_INTERNAL_SERVER_ERROR.getMessage()
            ));
    }
}
