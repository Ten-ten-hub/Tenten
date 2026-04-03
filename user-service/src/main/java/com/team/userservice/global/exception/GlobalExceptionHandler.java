package com.team.userservice.global.exception;

import com.team.userservice.global.domain.error.UserErrorCode;
import com.team.userservice.global.dto.CommonResponse;
import com.team.userservice.global.dto.CommonResponse.ValidationErrorDetail;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<List<ValidationErrorDetail>>> handleValidException(
        MethodArgumentNotValidException e) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(CommonResponse.onFailure(UserErrorCode.INVALID_REQUEST, e.getBindingResult()));
    }


}
