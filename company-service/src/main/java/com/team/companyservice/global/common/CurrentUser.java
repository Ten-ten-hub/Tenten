package com.team.companyservice.global.common;

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

    public boolean isHubDeliveryManager() {
        return "HUB_DELIVERY_MANAGER".equals(role);
    }

    public boolean isCompanyDeliveryManager() {
        return "COM_DELIVERY_MANAGER".equals(role);
    }
}
