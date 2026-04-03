package com.team.userservice.user.users.presentation.dto.request;

import com.team.userservice.user.users.application.dto.SignUpServiceDto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;


public record SignUpReq(
    @NotBlank(message = "성함을 입력해주세요.")
    String name,

    @NotBlank(message = "아이디는 필수입니다.")
    @Pattern(regexp = "^[a-z0-9]{4,10}$", message = "아이디는 4~10자의 영문 소문자와 숫자만 가능합니다.")
    String loginId,

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,15}$",
        message = "비밀번호는 8~15자, 대/소문자, 숫자, 특수문자를 포함해야 합니다.")
    String password,

    @NotBlank(message = "슬랙 아이디를 입력해주세요.")
    String slackId,

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    String email,

    @NotBlank(message = "연락 가능한 번호는 필수입니다.")
    String phoneNumber
) {
    public SignUpServiceDto toServiceDto() {
        return new SignUpServiceDto(name, loginId, password, slackId, email, phoneNumber);
    }
}
