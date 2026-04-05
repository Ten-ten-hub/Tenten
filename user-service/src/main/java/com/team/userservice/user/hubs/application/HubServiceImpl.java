package com.team.userservice.user.hubs.application;

import com.team.userservice.user.core.HubUser;
import com.team.userservice.user.core.User;
import com.team.userservice.user.hubs.domain.HubRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HubServiceImpl implements HubService {
    private final HubRepository hubRepository;

    @Override
    public void save(User user, UUID affiliationId) {
        hubRepository.save(user, affiliationId);
    }

    @Override
    public HubUser findByUser(User user) {
        return hubRepository.findByUser(user);
    }
}
