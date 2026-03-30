package com.team.common.exception;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        return ResponseEntity.status(e.getErrorCode().getStatus()).body(ErrorResponse.of(e.getErrorCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        List<Map<String, String>> details = e.getBindingResult().getFieldErrors().stream().map(
            error -> Map.of("field", error.getField(), "value",
                error.getRejectedValue() != null ? String.valueOf(error.getRejectedValue()) : "", "reason",
                String.valueOf(error.getDefaultMessage()))).toList();

        return ResponseEntity.status(CommonErrorCode.INVALID_INPUT.getStatus())
            .body(ErrorResponse.of(CommonErrorCode.INVALID_INPUT, details));
    }

}
