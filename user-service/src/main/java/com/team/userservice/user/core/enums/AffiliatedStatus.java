package com.team.userservice.user.core.enums;

//엔티티에서 배정을 받았는지 아직 못받은 상태인지 표현하기 위해 사용
public enum AffiliatedStatus {

    UNAFFILIATED("배정을 아직 못받은 상태 (권한은 있는데 소속이 없음)"),
    HUB_AFFILIATED("허브 배정을 이미 받은 상태"),
    COM_AFFILIATED("업체 배정을 이미 받은 상태"),
    NOT_APPLICABLE("배정을 받을 필요 없음 (EX. MASTER_ADMIN, NONE)");

    private final String description;

    AffiliatedStatus(String description) {
        this.description = description;
    }
}
