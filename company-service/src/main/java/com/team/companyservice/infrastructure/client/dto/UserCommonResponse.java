package com.team.companyservice.infrastructure.client.dto;

/**
 * user-service 공통 응답 포맷
 */
public record UserCommonResponse<T>(
    boolean success,
    T data,
    String code,
    String message
) {
}
