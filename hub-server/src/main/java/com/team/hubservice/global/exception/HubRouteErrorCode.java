package com.team.hubservice.global.exception;

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
    ROUTE_DUPLICATED("ROUTE_DUPLICATED", "출발 허브와 도착 허브 간의 경로가 이미 존재합니다.", HttpStatus.CONFLICT),
    TMAP_APP_KEY_MISSING("TMAP_APP_KEY_MISSING", "티맵 연동에 필요한 앱 키(TMAP_APP_KEY)가 설정되어 있지 않습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    TMAP_REQUEST_FAILED("TMAP_REQUEST_FAILED", "티맵 경로 API 호출에 실패했습니다.", HttpStatus.BAD_GATEWAY),
    TMAP_RESPONSE_INVALID("TMAP_RESPONSE_INVALID", "티맵 경로 API 응답에서 거리·시간 요약 정보를 확인할 수 없습니다.", HttpStatus.BAD_GATEWAY);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
