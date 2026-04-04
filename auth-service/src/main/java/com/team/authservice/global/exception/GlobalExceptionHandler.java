package com.team.authservice.global.exception;

import com.team.authservice.global.dto.CommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<CommonResponse<Void>> handleAuthException(AuthException e) {
        return ResponseEntity
            .status(e.getErrorCode().getStatus())
            .body(CommonResponse.onFailure(e.getErrorCode()));
    }
}
