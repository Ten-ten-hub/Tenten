package com.team.userservice.user.core.enums;

public enum Role {
    NONE("처음 가입 시 관리자가 권한 부여하기 전 상태 , 아무 권한 없는 상태"),
    MASTER_ADMIN("마스터 관리자"),
    HUB_ADMIN("허브 관리자"),
    HUB_DELIVERY_MANAGER("허브 배송 담당자"),
    COM_DELIVERY_MANAGER("업체 배송 담당자"),
    COMPANY_MANAGER("업체 담당자");

    private final String description;

    Role (String description) {
        this.description = description;
    }
}
