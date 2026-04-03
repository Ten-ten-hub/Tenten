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
import org.springframework.stereotype.Service;

@Service
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
        Role role = userInternalClient.getUserRole(userId).role();
        String newAccessToken = null;
        String newRefreshToken = null;

        //rt 에서 userId 추출하여 레디스에서 찾기
        String redisRt = redisTokenRepository.findByUserId(userId);
        if(refreshToken.equals(redisRt)){
            newAccessToken = jwtProvider.generateAccessToken(userId, role);
            newRefreshToken = jwtProvider.generateRefreshToken(userId);
            redisTokenRepository.save(userId, newRefreshToken); // 새 RT Redis에 저장
        }else{
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }
        return new TokenDto(newAccessToken, newRefreshToken); // 새 토큰 반환

    }
}
