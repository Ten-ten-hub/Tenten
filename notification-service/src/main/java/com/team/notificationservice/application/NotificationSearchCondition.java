package com.team.notificationservice.application;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationSearchCondition {
    private String slackId;
    private String keyword; // 메시지 내용 검색용 키워드 추가
}