package com.team.notificationservice.infrastructure;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.users.UsersLookupByEmailResponse;
import com.team.notificationservice.presentation.common.ErrorCode;
import com.team.notificationservice.presentation.common.ServiceException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SlackClient {

    @Value("${slack.token}")
    private String slackToken;

    private MethodsClient methodsClient;

    @PostConstruct
    public void init() {
        this.methodsClient = Slack.getInstance().methods(slackToken);
    }

    // 1. 이메일로 슬랙 ID(U...) 찾기
    public String findSlackIdByEmail(String email) {
        try {
            UsersLookupByEmailResponse response = methodsClient.usersLookupByEmail(r -> r.email(email));
            if (response.isOk()) {
                return response.getUser().getId();
            }

            if ("users_not_found".equals(response.getError())) {
                log.warn("이메일로 사용자를 찾을 수 없음: {}", maskEmail(email));
                return null;
            }

            throw new ServiceException(ErrorCode.NOTI_SLACK_API_ERROR);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("슬랙 이메일 조회 중 오류", e);
            throw new ServiceException(ErrorCode.NOTI_SLACK_API_ERROR, e);
        }
    }

    // 2. 메시지 전송 (최종 타겟 ID 사용)
    public boolean sendDirectMessage(String targetId, String text) {
        try {
            ChatPostMessageResponse response = methodsClient.chatPostMessage(r -> r
                .channel(targetId)
                .text(text)
            );
            return response.isOk();
        } catch (Exception e) {
            log.error("슬랙 발송 오류", e);
            return false;
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "UNKNOWN";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];

        if (local.isEmpty()) {
            return "UNKNOWN";
        }
        if (local.length() == 1) {
            return "*" + "@" + domain;
        }

        return local.charAt(0) + "*".repeat(local.length() - 1) + "@" + domain;
    }
}
