package com.team.notificationservice.infrastructure;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.users.UsersLookupByEmailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SlackClient {

    @Value("${slack.token}")
    private String slackToken;

    // 1. 이메일로 슬랙 ID(U...) 찾기
    public String findSlackIdByEmail(String email) {
        try {
            MethodsClient client = Slack.getInstance().methods(slackToken);
            UsersLookupByEmailResponse response = client.usersLookupByEmail(r -> r.email(email));
            if (response.isOk()) {
                return response.getUser().getId();
            }
            log.warn("이메일로 사용자를 찾을 수 없음: {}", email);
        } catch (Exception e) {
            log.error("슬랙 이메일 조회 중 오류", e);
        }
        return null;
    }

    // 2. 메시지 전송 (최종 타겟 ID 사용)
    public boolean sendDirectMessage(String targetId, String text) {
        try {
            MethodsClient client = Slack.getInstance().methods(slackToken);
            ChatPostMessageResponse response = client.chatPostMessage(r -> r
                    .channel(targetId)
                    .text(text)
            );
            return response.isOk();
        } catch (Exception e) {
            log.error("슬랙 발송 오류", e);
            return false;
        }
    }
}
