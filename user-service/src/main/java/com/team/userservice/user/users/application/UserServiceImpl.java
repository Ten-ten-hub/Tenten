package com.team.userservice.user.users.application;

import com.team.userservice.global.domain.error.UserErrorCode;
import com.team.userservice.global.exception.UserException;
import com.team.userservice.user.core.User;
import com.team.userservice.user.core.enums.Role;
import com.team.userservice.user.users.application.dto.LoginServiceDto;
import com.team.userservice.user.users.application.dto.SignUpResultDto;
import com.team.userservice.user.users.application.dto.SignUpServiceDto;
import com.team.userservice.user.users.domain.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public SignUpResultDto signUp(SignUpServiceDto serviceDto) {

        if (userRepository.existsByLoginId(serviceDto.loginId())) {
            throw new UserException(UserErrorCode.DUPLICATE_LOGIN_ID);
        }
        if (userRepository.existsByEmail(serviceDto.email())) {
            throw new UserException(UserErrorCode.DUPLICATE_EMAIL);
        }

        User user = userRepository.save(User.create()
                .loginId(serviceDto.loginId())
                .password(serviceDto.password())
                .name(serviceDto.name())
                .role(serviceDto.role())
                .slackId(serviceDto.slackId())
                .email(serviceDto.email())
                .phoneNumber(serviceDto.phoneNumber())
                .build());

        return SignUpResultDto.from(user);
    }

    @Override
    @Transactional
    public void register(UUID userId, Role giveRole) {
        userRepository.register(userId, giveRole);
    }

    @Override
    public LoginServiceDto findByLoginId(String loginId) {
        return LoginServiceDto.from(userRepository.findByLoginId(loginId));
    }
}
