package com.team.notificationservice.application;

import java.util.List;
import java.util.UUID;

public record AiNotificationRequest(
    UUID orderId,
    String receiverSlackId,      // 발송 허브 담당자 슬랙 ID
    String receiverId,           // 수신자 UUID
    String productName,          // 상품 정보 (예: 마른 오징어 50박스)
    String orderRequestDetails,  // 요청 사항 (예: 12월 12일 3시까지 보내주세요)
    String originAddress,        // 발송지 (경기 북부 센터)
    List<String> waypoints,      // 경유지 리스트 (대전, 부산 센터 등)
    String destinationAddress,   // 도착지 주소
    String deliveryManagerName,  // 배송 담당자 이름
    String workingHours          // 근무 시간 (09 - 18)
) {
}
