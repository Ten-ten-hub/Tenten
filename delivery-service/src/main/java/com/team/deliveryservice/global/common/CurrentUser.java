package com.team.deliveryservice.global.common;

import java.util.UUID;

public record CurrentUser(
    UUID userId,
    String role,
    UUID hubId,
    UUID companyId
) {
    public boolean isMasterAdmin() {
        return "MASTER_ADMIN".equalsIgnoreCase(role);
    }

    public boolean isHubAdmin() {
        return "HUB_ADMIN".equalsIgnoreCase(role);
    }

    public boolean isCompanyManager() {
        return "COMPANY_MANAGER".equalsIgnoreCase(role);
    }

    public boolean isHubDeliveryManager() {
        return "HUB_DELIVERY_MANAGER".equalsIgnoreCase(role);
    }

    public boolean isCompanyDeliveryManager() {
        return "COM_DELIVERY_MANAGER".equalsIgnoreCase(role);
    }
}
