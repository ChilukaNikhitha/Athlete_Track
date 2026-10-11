package com.athletetrack.tournament;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentFixtureRepository extends JpaRepository<TournamentFixture, Long> {
    List<TournamentFixture> findTop100ByCoachIdOrderByScheduledAtAscIdAsc(Long coachId);
    List<TournamentFixture> findTop100ByPlayerIdOrderByScheduledAtAscIdAsc(Long playerId);
}
