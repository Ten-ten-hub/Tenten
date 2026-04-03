package com.team.authservice.auth.application;

import com.team.authservice.auth.application.dto.TokenDto;
import java.util.UUID;

public interface AuthService {
    TokenDto login(String loginId, String password);

    void logout(UUID userId);

    TokenDto refresh(String refreshToken);
}
