package com.team.userservice.user.hub.infrastructure;

import com.team.userservice.global.domain.error.UserErrorCode;
import com.team.userservice.global.exception.UserException;
import com.team.userservice.user.core.HubUser;
import com.team.userservice.user.core.User;
import com.team.userservice.user.hub.domain.HubRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class HubRepositoryImpl implements HubRepository {
    private final HubJpaRepository hubJpaRepository;

    @Override
    public void update(User user, UUID affiliationId) {
        hubJpaRepository.findByUser(user)
            .ifPresentOrElse(
                hubUser -> hubUser.updateHubId(affiliationId),
                () -> hubJpaRepository.save(HubUser.create(user, affiliationId))
            );
    }

    @Override
    public HubUser findByUser(User user) {
        return hubJpaRepository.findByUser(user).orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    }
}
