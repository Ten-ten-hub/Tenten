package com.team.companyservice.infrastructure.client.dto;

import java.util.UUID;

/**
 * user-service 내부 API 계약에 맞춘 응답 DTO
 * 내부 연동 계약이므로 필드 변경 시 양쪽 서비스를 함께 수정해야 한다.
 */
public record UserInternalResponse(
    UUID userId,
    String role,
    String signupStatus,
    String affiliatedStatus,
    UUID affiliationId
) {
}
