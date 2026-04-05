package com.team.companyservice.infrastructure.client.dto;

public record UserCommonResponse<T>(
    boolean success,
    T data,
    String code,
    String message
) {
}
