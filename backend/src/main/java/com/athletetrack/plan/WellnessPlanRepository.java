package com.athletetrack.plan;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WellnessPlanRepository extends JpaRepository<WellnessPlan,Long> {
    List<WellnessPlan> findTop100ByPlayerIdOrderByCreatedAtDesc(Long playerId);
    List<WellnessPlan> findTop100ByCoachIdOrderByCreatedAtDesc(Long coachId);
}
