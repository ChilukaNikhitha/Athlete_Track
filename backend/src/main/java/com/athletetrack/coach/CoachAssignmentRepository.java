package com.athletetrack.coach;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoachAssignmentRepository extends JpaRepository<CoachAssignment,Long> {
    List<CoachAssignment> findAllByCoachIdOrderByAssignedAtDesc(Long coachId);
    boolean existsByCoachIdAndPlayerId(Long coachId,Long playerId);
}
