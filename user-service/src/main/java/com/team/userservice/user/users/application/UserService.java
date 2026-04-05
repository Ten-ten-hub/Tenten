package com.team.userservice.user.users.application;

import com.team.userservice.user.core.User;
import com.team.userservice.user.core.enums.AffiliatedStatus;
import com.team.userservice.user.core.enums.Affiliation;
import com.team.userservice.user.core.enums.Role;
import com.team.userservice.user.core.enums.SignupStatus;
import com.team.userservice.user.users.application.dto.LoginServiceDto;
import com.team.userservice.user.users.application.dto.SignUpResultDto;
import com.team.userservice.user.users.application.dto.SignUpServiceDto;
import com.team.userservice.user.users.application.dto.UpdateUserServiceDto;
import com.team.userservice.user.users.application.dto.UserDataDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    SignUpResultDto signUp(SignUpServiceDto serviceDto);

    void register(UUID userId);

    LoginServiceDto loginService(String loginId, String password);

    void updateUserRole(@NotNull UUID userId, @NotNull Role role);

    void userUpdate(UUID userId, @Valid UpdateUserServiceDto serviceDto);

    void updateUserAffiliation(UUID userId, @NotNull Affiliation affiliation, @NotNull UUID uuid);

    UserDataDto getUserInfo(UUID userId);

    Page<User> getAllUserInfo(Pageable pageable);

    Page<User> getAllUserInfo(List<Role> roles, AffiliatedStatus affiliatedStatus, Pageable pageable);

    List<User> getAllUserInfoInternal();

    List<User> getAllUserInfoInternal(List<Role> roles, AffiliatedStatus affiliatedStatus);

    Page<User> getAllUserInfoBySignUpStatus(SignupStatus signupStatus, Pageable pageable);

    void deleteUser(UUID targetId, UUID deletedBy);

    void updateLastLoginAt(UUID userId);

    Role getUserRole(UUID userId);

    void verifyAffiliationId(@NotNull Affiliation affiliation, @NotNull UUID uuid);

    String getUserSlackId(UUID userId);
}
