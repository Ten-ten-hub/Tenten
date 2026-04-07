package com.team.deliveryservice.infrastructure.client.dto;

public record CompanyResponseWrapper(
    boolean success,
    CompanyInternalResponse data,
    String code,
    String message
) {
}
