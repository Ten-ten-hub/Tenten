package com.team.hubservice.hub.repository;

import com.team.hubservice.hub.domain.Hub;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HubRepository extends JpaRepository<Hub, UUID> {
    boolean existsByName(String name);

    Page<Hub> findByNameContaining(String name, Pageable pageable);
}