package com.team.authservice.auth.presentation;

import com.team.authservice.auth.application.AuthService;
import com.team.authservice.auth.application.dto.TokenDto;
import com.team.authservice.auth.presentation.dto.request.LoginReq;
import com.team.authservice.auth.presentation.dto.response.LoginRes;
import com.team.authservice.auth.presentation.dto.response.RefreshRes;
import com.team.authservice.auth.security.jwt.JwtProperties;
import com.team.authservice.global.dto.CommonResponse;
import com.team.authservice.global.error.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.Cookie;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    @PostMapping("/login")
    public ResponseEntity<CommonResponse<LoginRes>> login(
        @RequestBody LoginReq loginReqDto,
        HttpServletResponse response
    ) {
        TokenDto tokens = authService.login(loginReqDto.loginId(), loginReqDto.password());

        ResponseCookie cookie = ResponseCookie.from("refreshToken", tokens.refreshToken()).httpOnly(true).path("/")
            .maxAge(jwtProperties.refreshTokenValidity()).build();
        response.addHeader("Set-Cookie", cookie.toString());

        return ResponseEntity.ok(CommonResponse.onSuccess(new LoginRes(tokens.accessToken())));
    }

    //로그 아웃 - 레디스에 남은 리프레시 토큰 삭제
    @DeleteMapping("/logout")
    public ResponseEntity<CommonResponse<Void>> logout(HttpServletRequest request, HttpServletResponse response) {
        String userIdHeader = request.getHeader("X-User-Id");
        if(userIdHeader == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        UUID userId;
        try{
            userId = UUID.fromString(userIdHeader);
        }catch (IllegalArgumentException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        authService.logout(userId);

        ResponseCookie expiredCookie = ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .path("/")
            .maxAge(0)
            .build();

        response.addHeader("Set-Cookie", expiredCookie.toString());
        return ResponseEntity.ok(CommonResponse.onSuccess());
    }

    //토큰 재발급
    @PatchMapping("/refresh")
    public ResponseEntity<CommonResponse<?>> refresh(HttpServletRequest request, HttpServletResponse response) {

        if(request.getCookies() == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(CommonResponse.onFailure(AuthErrorCode.INVALID_TOKEN));
        }

        String refreshToken = Arrays.stream(request.getCookies())
            .filter(c -> "refreshToken".equals(c.getName()))
            .findFirst()
            .map(Cookie::getValue)
            .orElse(null);

        if(refreshToken == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(CommonResponse.onFailure(AuthErrorCode.INVALID_TOKEN));
        }

        TokenDto tokens = authService.refresh(refreshToken);
        ResponseCookie cookie = ResponseCookie.from("refreshToken", tokens.refreshToken())
            .httpOnly(true)
            .path("/")
            .maxAge(jwtProperties.refreshTokenValidity())
            .sameSite("Lax")
            .build();
        response.addHeader("Set-Cookie", cookie.toString());

        return ResponseEntity.ok(CommonResponse.onSuccess(new RefreshRes(tokens.accessToken())));
    }

}
