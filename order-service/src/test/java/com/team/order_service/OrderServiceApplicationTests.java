package com.team.order_service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("로컬 DB 설정 없이 실행 시 컨텍스트 로드 실패")
class OrderServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
