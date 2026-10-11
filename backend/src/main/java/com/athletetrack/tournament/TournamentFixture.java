package com.athletetrack.tournament;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "tournament_fixtures")
public class TournamentFixture {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long playerId;

    @Column(nullable = false)
    private Long coachId;

    @Column(nullable = false, length = 120)
    private String tournamentName;

    @Column(nullable = false, length = 120)
    private String opponent;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Column(nullable = false, length = 120)
    private String venue;

    @Column(length = 80)
    private String stage;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected TournamentFixture() {}

    public TournamentFixture(Long playerId, Long coachId, String tournamentName, String opponent,
                             LocalDateTime scheduledAt, String venue, String stage, String notes) {
        this.playerId = playerId;
        this.coachId = coachId;
        this.tournamentName = tournamentName;
        this.opponent = opponent;
        this.scheduledAt = scheduledAt;
        this.venue = venue;
        this.stage = stage;
        this.notes = notes;
    }

    public Long getId() { return id; }
    public Long getPlayerId() { return playerId; }
    public Long getCoachId() { return coachId; }
    public String getTournamentName() { return tournamentName; }
    public String getOpponent() { return opponent; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public String getVenue() { return venue; }
    public String getStage() { return stage; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
}
