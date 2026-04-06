package com.team.aiservice.domain.repository;

import com.team.aiservice.domain.model.AiAnalysis;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, UUID> {
}
