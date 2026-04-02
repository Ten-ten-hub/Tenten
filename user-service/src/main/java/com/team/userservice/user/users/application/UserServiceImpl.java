package com.team.userservice.user.users.application;

import com.team.userservice.global.domain.error.UserErrorCode;
import com.team.userservice.global.exception.UserException;
import com.team.userservice.user.core.CompanyUser;
import com.team.userservice.user.core.HubUser;
import com.team.userservice.user.core.User;
import com.team.userservice.user.core.enums.Affiliation;
import com.team.userservice.user.core.enums.Role;
import com.team.userservice.user.core.enums.SignupStatus;
import com.team.userservice.user.core.vo.UserUpdateInfo;
import com.team.userservice.user.users.application.dto.LoginServiceDto;
import com.team.userservice.user.users.application.dto.SignUpResultDto;
import com.team.userservice.user.users.application.dto.SignUpServiceDto;
import com.team.userservice.user.users.application.dto.UpdateUserServiceDto;
import com.team.userservice.user.users.application.dto.UserDataDto;
import com.team.userservice.user.users.domain.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
                .role(Role.NONE)
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
    public void register(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        user.register();
    }

    @Override
    public LoginServiceDto loginService(String loginId, String password) {
        User user = userRepository.findByLoginId(loginId);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UserException(UserErrorCode.INVALID_CREDENTIALS);
        }
        return LoginServiceDto.from(user.getId(), user.getRole());
    }

    @Override
    @Transactional
    public void updateUserRole(UUID userId, Role role) {
        userRepository.findById(userId)
            .ifPresentOrElse(
                user -> userRepository.updateUserRole(user, role),
                () -> {
                    throw new UserException(UserErrorCode.USER_NOT_FOUND);
                }
            );
    }

    @Override
    @Transactional
    public void userUpdate(UUID userId, UpdateUserServiceDto serviceDto) {
        UserUpdateInfo info = serviceDto.toEntityDto();
        userRepository.findById(userId)
            .ifPresentOrElse(
                user -> user.userUpdate(info),
                () -> {
                    throw new UserException(UserErrorCode.USER_NOT_FOUND);
                }
            );
    }

    @Override
    @Transactional
    public void updateUserAffiliation(UUID userId, Affiliation affiliation, UUID affiliationId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        Role role = user.getRole();
        if (role == Role.NONE || role == Role.MASTER_ADMIN) {
            throw new UserException(UserErrorCode.INVALID_REQUEST);
        } else if (role == Role.HUB_ADMIN || role == Role.HUB_DELIVERY_MANAGER) {
            if (affiliation == Affiliation.COMPANY) {
                throw new UserException(UserErrorCode.ROLE_AFFILIATION_CONFLICT);
            }
            userRepository.updateHubUser(user, affiliationId);
        } else if (role == Role.COMPANY_MANAGER || role == Role.COM_DELIVERY_MANAGER) {
            if (affiliation == Affiliation.HUB) {
                throw new UserException(UserErrorCode.ROLE_AFFILIATION_CONFLICT);
            }
            userRepository.updateCompanyUser(user, affiliationId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserDataDto getUserInfo(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        Role role = user.getRole();
        if (role == Role.HUB_ADMIN || role == Role.HUB_DELIVERY_MANAGER) {
            HubUser hubUserData = userRepository.findHubUser(user);
            return UserDataDto.fromUserInfo(user, Affiliation.HUB, hubUserData.getHubId());
        } else if (role == Role.COM_DELIVERY_MANAGER || role == Role.COMPANY_MANAGER) {
            CompanyUser companyUserData = userRepository.findCompanyUser(user);
            return UserDataDto.fromUserInfo(user, Affiliation.COMPANY,
                companyUserData.getCompanyId());
        } else if (role == Role.MASTER_ADMIN || role == Role.NONE) {
            return UserDataDto.fromMaster(user);
        } else {
            throw new UserException(UserErrorCode.INVALID_REQUEST);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getAllUserInfo(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUserInfoInternal() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getAllUserInfoBySignUpStatus(SignupStatus signupStatus, Pageable pageable) {
        return userRepository.findAllBySignupStatus(signupStatus, pageable);
    }

    @Override
    @Transactional
    public void deleteUser(UUID targetId, UUID deletedBy) {
        userRepository.findById(targetId).ifPresentOrElse(
            user -> user.deleteUser(deletedBy),
            () -> {
                throw new UserException(UserErrorCode.USER_NOT_FOUND);
            }
        );
    }

    @Override
    @Transactional
    public void updateLastLoginAt(UUID userId) {
        userRepository.findById(userId).ifPresentOrElse(
            user -> user.updateLastLoginAt(LocalDateTime.now()),
            () -> {
                throw new UserException(UserErrorCode.USER_NOT_FOUND);
            }
        );
    }
}
