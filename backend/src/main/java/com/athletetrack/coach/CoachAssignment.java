package com.athletetrack.coach;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="coach_assignments",uniqueConstraints=@UniqueConstraint(columnNames={"coach_id","player_id"}))
public class CoachAssignment {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="coach_id",nullable=false) private Long coachId;
    @Column(name="player_id",nullable=false) private Long playerId;
    @Column(nullable=false,updatable=false) private Instant assignedAt=Instant.now();
    protected CoachAssignment(){}
    public CoachAssignment(Long coachId,Long playerId){this.coachId=coachId;this.playerId=playerId;}
    public Long getId(){return id;} public Long getCoachId(){return coachId;} public Long getPlayerId(){return playerId;}
    public Instant getAssignedAt(){return assignedAt;}
}
