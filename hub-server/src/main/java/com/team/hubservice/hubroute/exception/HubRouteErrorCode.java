package com.team.hubservice.hubroute.exception;

import com.team.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum HubRouteErrorCode implements ErrorCode {
    SAME_HUB_NOT_ALLOWED("ROUTE_SAME_HUB_NOT_ALLOWED", "출발 허브와 도착 허브는 동일할 수 없습니다.", HttpStatus.BAD_REQUEST),
    NEGATIVE_VALUE_NOT_ALLOWED("ROUTE_NEGATIVE_VALUE_NOT_ALLOWED", "소요 시간 및 이동 거리는 0보다 작을 수 없습니다.", HttpStatus.BAD_REQUEST),
    ROUTE_NOT_FOUND("ROUTE_NOT_FOUND", "요청한 허브 간 이동 경로를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ROUTE_HUB_NOT_FOUND("ROUTE_HUB_NOT_FOUND", "경로 등록에 지정된 출발 허브 또는 도착 허브를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    OPTIMAL_NOT_FOUND("ROUTE_OPTIMAL_NOT_FOUND", "출발 허브에서 도착 허브로 가는 최적 경로 구간을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ROUTE_DUPLICATED("ROUTE_DUPLICATED", "출발 허브와 도착 허브 간의 경로가 이미 존재합니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
