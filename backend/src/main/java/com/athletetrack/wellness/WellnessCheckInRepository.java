package com.athletetrack.wellness;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WellnessCheckInRepository extends JpaRepository<WellnessCheckIn,Long> {
    List<WellnessCheckIn> findTop30ByPlayerIdOrderByRecordedAtDescIdDesc(Long playerId);
}
