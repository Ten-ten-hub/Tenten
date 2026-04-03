package com.team.userservice.user.core.enums;

public enum SignupStatus {
    PENDING("가입 승인 요청 중 ( 로그인 안되는 상태 && 초기 권한 == NONE )"),
    APPROVED("가입 승인 상태"),
    REJECTED("가입 거절"),
    INACTIVE("비활성화 또는 활동 정지");

    private final String description;
    SignupStatus(String description) {
        this.description = description;
    }
}
