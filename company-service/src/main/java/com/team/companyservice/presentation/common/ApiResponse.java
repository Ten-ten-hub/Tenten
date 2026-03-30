package com.team.companyservice.presentation.common;

public record ApiResponse<T>(
        boolean success,
        T data,
        String code,
        String message
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, "SUCCESS", "요청이 성공했습니다.");
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, "SUCCESS", "요청이 성공했습니다.");
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(false, null, code, message);
    }
}