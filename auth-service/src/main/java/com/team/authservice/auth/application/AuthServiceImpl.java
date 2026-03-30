package com.team.authservice.auth.application;

import com.team.authservice.auth.application.dto.TokenDto;
import com.team.authservice.auth.infrastructure.RedisTokenRepository;
import com.team.authservice.auth.infrastructure.feign.UserInternalClient;
import com.team.authservice.auth.infrastructure.feign.dto.UserVerifyReqDto;
import com.team.authservice.auth.infrastructure.feign.dto.UserVerifyResDto;
import com.team.authservice.auth.security.jwt.JwtProvider;
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
        UserVerifyResDto userInfo = userInternalClient.verify(new UserVerifyReqDto(loginId, password));
        String accessToken = jwtProvider.generateAccessToken(userInfo.userId(), userInfo.role());
        String refreshToken = jwtProvider.generateRefreshToken(userInfo.userId());

        redisTokenRepository.save(userInfo.userId(), refreshToken);

        return new TokenDto(accessToken, refreshToken);
    }
}
