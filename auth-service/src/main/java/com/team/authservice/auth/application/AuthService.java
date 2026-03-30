package com.team.authservice.auth.application;

import com.team.authservice.auth.application.dto.TokenDto;

public interface AuthService {
    TokenDto login(String loginId, String password);
}
