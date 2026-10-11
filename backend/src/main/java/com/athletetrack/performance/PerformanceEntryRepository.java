package com.athletetrack.performance;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerformanceEntryRepository extends JpaRepository<PerformanceEntry,Long> {
    List<PerformanceEntry> findTop50ByPlayerIdOrderByRecordedAtDescIdDesc(Long playerId);
}
