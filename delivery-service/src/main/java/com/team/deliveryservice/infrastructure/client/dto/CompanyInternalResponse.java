package com.team.deliveryservice.infrastructure.client.dto;

import java.util.UUID;

public record CompanyInternalResponse(
    UUID id,
    String name,
    String companyType,
    UUID hubId,
    String address,
    String addressDetail,
    String contactName,
    String contactSlackId,
    boolean active
) {
}
