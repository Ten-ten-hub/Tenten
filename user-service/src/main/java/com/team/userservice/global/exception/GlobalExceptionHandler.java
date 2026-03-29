package com.team.userservice.global.exception;

import com.team.userservice.global.dto.CommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(UserException.class)
    public ResponseEntity<CommonResponse<Void>> handleUserException(UserException e) {
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(CommonResponse.onFailure(e.getErrorCode()));
    }

}
