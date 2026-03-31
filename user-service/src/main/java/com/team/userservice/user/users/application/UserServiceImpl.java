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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public SignUpResultDto signUp(SignUpServiceDto serviceDto) {

        if (userRepository.existsByLoginId(serviceDto.loginId())) {
            throw new UserException(UserErrorCode.DUPLICATE_LOGIN_ID);
        }
        if (userRepository.existsByEmail(serviceDto.email())) {
            throw new UserException(UserErrorCode.DUPLICATE_EMAIL);
        }

        try {
            User user = userRepository.save(User.create()
                .loginId(serviceDto.loginId())
                .password(passwordEncoder.encode(serviceDto.password()))
                .name(serviceDto.name())
                .role(serviceDto.role())
                .slackId(serviceDto.slackId())
                .email(serviceDto.email())
                .phoneNumber(serviceDto.phoneNumber())
                .build());
            userRepository.flush();
            return SignUpResultDto.from(user);
        } catch (DataIntegrityViolationException e) {
            throw new UserException(UserErrorCode.DUPLICATE_USER_INFO);
        }


    }

    @Override
    @Transactional
    public void register(UUID userId, Role giveRole) {
        userRepository.register(userId, giveRole);
    }

    @Override
    public LoginServiceDto loginService(String loginId, String password) {
        User user = userRepository.findByLoginId(loginId);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UserException(UserErrorCode.INVALID_CREDENTIALS);
        }
        return LoginServiceDto.from(user.getId(), user.getRole());
    }
}
