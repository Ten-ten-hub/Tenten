package com.team.notificationservice.presentation.common;

import com.team.notificationservice.presentation.common.ApiResponse.ValidationError;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

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
        log.error("ValidationException: {}", e.getMessage());

        List<ValidationError> errors = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new ApiResponse.ValidationError(
                error.getField(),
                String.valueOf(error.getRejectedValue()),
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
