package com.team.notificationservice.presentation.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

public record ApiResponse<T>(
    boolean success,
    T data,
    String code,
    String message,
    @JsonInclude(JsonInclude.Include.NON_NULL) //에러가 없을 땐 응답에서 제외
    List<ValidationError> errors
) {
    private static final String DEFAULT_SUCCESS_MESSAGE = "요청에 성공하였습니다.";

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, "SUCCESS", DEFAULT_SUCCESS_MESSAGE, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, "SUCCESS", DEFAULT_SUCCESS_MESSAGE, null);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(false, null, code, message, null);
    }

    public static ApiResponse<Void> error(String code, String message, List<ValidationError> errors) {
        return new ApiResponse<>(false, null, code, message, errors);
    }

    public record ValidationError(String field, String value, String reason) {
    }
}
