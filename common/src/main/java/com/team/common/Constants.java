package com.team.common;

import java.util.UUID;

public class Constants {
    public static final UUID SYSTEM_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    public static final String SYSTEM_USER_ID = "SYSTEM";
    // 헤더가 없거나 잘못된 요청인데 일단 저장해야 할 때 (추적용)
    public static final UUID UNKNOWN_USER_UUID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
}
