package com.athletetrack.profile;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerProfileRepository extends JpaRepository<PlayerProfile,Long> {
    @EntityGraph(attributePaths="user")
    Optional<PlayerProfile> findByUserId(Long userId);
}
