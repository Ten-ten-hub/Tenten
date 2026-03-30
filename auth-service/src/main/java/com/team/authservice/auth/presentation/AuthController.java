package com.team.authservice.auth.presentation;

import com.team.authservice.auth.application.AuthService;
import com.team.authservice.auth.application.dto.TokenDto;
import com.team.authservice.auth.presentation.dto.request.LoginReqDto;
import com.team.authservice.auth.presentation.dto.response.LoginResDto;
import com.team.authservice.auth.security.jwt.JwtProperties;
import com.team.authservice.global.dto.CommonResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    @PostMapping("/login")
    public ResponseEntity<CommonResponse<LoginResDto>> login(
        @RequestBody LoginReqDto loginReqDto,
        HttpServletResponse response
    ) {
        TokenDto tokens = authService.login(loginReqDto.loginId(), loginReqDto.password());

        ResponseCookie cookie = ResponseCookie.from("refreshToken", tokens.refreshToken()).httpOnly(true).path("/")
            .maxAge(jwtProperties.refreshTokenValidity()).build();
        response.addHeader("Set-Cookie", cookie.toString());

        return ResponseEntity.ok(CommonResponse.onSuccess(new LoginResDto(tokens.accessToken())));
    }
}
