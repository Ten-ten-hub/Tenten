package com.team.authservice.auth.application;

import com.team.authservice.auth.application.dto.TokenDto;
import com.team.authservice.auth.infrastructure.RedisTokenRepository;
import com.team.authservice.auth.infrastructure.feign.UserInternalClient;
import com.team.authservice.auth.infrastructure.feign.dto.UserVerifyReq;
import com.team.authservice.auth.infrastructure.feign.dto.UserVerifyRes;
import com.team.authservice.auth.security.jwt.JwtProvider;
import com.team.authservice.core.enums.Role;
import com.team.authservice.global.error.AuthErrorCode;
import com.team.authservice.global.exception.AuthException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserInternalClient userInternalClient;
    private final JwtProvider jwtProvider;
    private final RedisTokenRepository redisTokenRepository;

    @Override
    public TokenDto login(String loginId, String password) {
        UserVerifyRes userInfo = userInternalClient.verify(new UserVerifyReq(loginId, password));
        String accessToken = jwtProvider.generateAccessToken(userInfo.userId(), userInfo.role());
        String refreshToken = jwtProvider.generateRefreshToken(userInfo.userId());

        redisTokenRepository.save(userInfo.userId(), refreshToken);

        try{
            userInternalClient.lastLoginAt(userInfo.userId()); // 부가기능이므로 실패해도 로그인은 정사응로 되어야함
        }catch (Exception e){
            log.warn("lastLoginAt 업데이트 실패 (userId = {}): {}", userInfo.userId(), e.getMessage());
        }

        return new TokenDto(accessToken, refreshToken);
    }

    @Override
    public void logout(UUID userId){
        redisTokenRepository.delete(userId);
    }

    @Override
    public TokenDto refresh(String refreshToken) {
        UUID userId;
        try{
            userId = jwtProvider.extractUserInfo(refreshToken);
        }catch (Exception e){
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }

        Role role;
        try{
            role = userInternalClient.getUserRole(userId).role();
        }catch (Exception e){
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }
        String newAccessToken = jwtProvider.generateAccessToken(userId, role);
        String newRefreshToken = jwtProvider.generateRefreshToken(userId);

        // compareAndReplace: 원자적으로 "기존 RT == Redis RT"이면 새 RT로 교체
        if (!redisTokenRepository.compareAndReplace(userId, refreshToken, newRefreshToken)) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
            // -> 이미 다른 요청이 RT를 교체했거나, RT가 일치하지 않는 경우
        }

        return new TokenDto(newAccessToken, newRefreshToken); // 새 토큰 반환

    }


}
