package com.team.aiservice.domain.repository;

import com.team.aiservice.domain.model.AiAnalysis;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, UUID> {

    Page<AiAnalysis> findAllByDeletedAtIsNull(Pageable pageable);

    Page<AiAnalysis> findByOrderIdAndDeletedAtIsNull(UUID orderId, Pageable pageable);

    Optional<AiAnalysis> findByIdAndDeletedAtIsNull(UUID id);
}
