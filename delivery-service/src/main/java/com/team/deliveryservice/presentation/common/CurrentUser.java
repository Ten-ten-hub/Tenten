package com.team.deliveryservice.presentation.common;

import java.util.UUID;

public record CurrentUser(
    UUID userId,
    String role,
    UUID hubId,
    UUID companyId
) {
    public boolean isMasterAdmin() {
        return "MASTER_ADMIN".equals(role);
    }

    public boolean isHubAdmin() {
        return "HUB_ADMIN".equals(role);
    }

    public boolean isCompanyManager() {
        return "COMPANY_MANAGER".equals(role);
    }

    public boolean isDeliveryManager() {
        return "DELIVERY_MANAGER".equals(role);
    }
}
