package com.team.hubservice.global.exception;

import com.team.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum HubErrorCode implements ErrorCode {
    HUB_ALREADY_EXISTS("HUB_ALREADY_EXISTS", "이미 동일한 이름의 허브가 존재합니다.", HttpStatus.CONFLICT),
    HUB_NOT_FOUND("HUB_NOT_FOUND", "요청한 허브 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus status;
}

