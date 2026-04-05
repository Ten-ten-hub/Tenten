package com.team.userservice.global.domain.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum UserErrorCode implements com.team.common.exception.ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "USER_400", "요청 값이 올바르지 않습니다."),

    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "USER_401", "아이디 또는 비밀번호가 일치하지 않습니다."),
    ROLE_AFFILIATION_CONFLICT(HttpStatus.BAD_REQUEST, "USER_400_1", "권한과 소속이 일치하지 않습니다."),
    NOT_APPLICABLE(HttpStatus.BAD_REQUEST,"USER_400_2" ,"배정을 받을 수 있는 상태가 아닙니다"),

    FORBIDDEN(HttpStatus.FORBIDDEN, "USER_403", "접근 권한이 없습니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404", "존재하지 않는 유저입니다."),
    HUB_USER_NOT_FOUND(HttpStatus.NOT_FOUND,"USER_404_1", "유저의 허브관련 정보가 존재하지 않습니다."),
    COM_USER_NOT_FOUND(HttpStatus.NOT_FOUND,"USER_404_2", "유저의 업체관련 정보가 존재하지 않습니다."),
    NOT_EXIST_ROLE(HttpStatus.NOT_FOUND, "USER_404_3","존재하지 않는 권한입니다." ),
    INVALID_HUB_ID(HttpStatus.NOT_FOUND, "USER_404_4", "존재하지 않는 허브ID입니다"),
    INVALID_COMPANY_ID(HttpStatus.NOT_FOUND, "USER_404_5", "존재하지 않는 업체ID입니다"),
    NOT_EXIST_AFFILIATION(HttpStatus.NOT_FOUND, "USER_404_6", "존재하지 않는 소속이름입니다" ),

    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "USER_409_1", "이미 사용 중인 아이디입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER_409_2", "이미 사용 중인 이메일입니다."),
    ALREADY_REGISTERED_USER(HttpStatus.CONFLICT, "USER_409_3", "이미 등록이 완료된 유저입니다."),
    DUPLICATE_USER_INFO(HttpStatus.CONFLICT, "USER_409_4", "이미 사용 중인 정보입니다.");


;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
