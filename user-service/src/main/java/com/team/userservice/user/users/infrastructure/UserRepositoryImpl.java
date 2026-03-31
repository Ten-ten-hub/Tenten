package com.team.userservice.user.users.infrastructure;

import com.team.userservice.global.domain.error.UserErrorCode;
import com.team.userservice.global.exception.UserException;
import com.team.userservice.user.core.User;
import com.team.userservice.user.core.enums.Role;
import com.team.userservice.user.users.domain.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {


    private final UserJpaRepository userJpaRepository;

    @Override
    public User save(User user) {
        return userJpaRepository.save(user);
    }

    @Override
    public void register(UUID userId, Role giveRole) {
        User user = userJpaRepository.findById(userId).orElseThrow(
            () -> new UserException(UserErrorCode.USER_NOT_FOUND));
        user.register(giveRole);
    }

    @Override
    public boolean existsByLoginId(String loginId) {
        return userJpaRepository.existsByLoginId(loginId);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public User findByLoginId(String loginId) {
        return userJpaRepository.findByLoginId(loginId)
            .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    }

    @Override
    public void flush() {
        userJpaRepository.flush();
    }
}
