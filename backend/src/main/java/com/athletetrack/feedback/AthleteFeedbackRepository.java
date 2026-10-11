package com.athletetrack.feedback;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AthleteFeedbackRepository extends JpaRepository<AthleteFeedback,Long> {
    List<AthleteFeedback> findTop100ByPlayerIdOrderByCreatedAtDesc(Long playerId);
    List<AthleteFeedback> findTop100ByCoachIdOrderByCreatedAtDesc(Long coachId);
}
