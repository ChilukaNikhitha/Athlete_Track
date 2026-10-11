package com.athletetrack.feedback;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="athlete_feedback")
public class AthleteFeedback {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long playerId;
    @Column(nullable=false) private Long coachId;
    @Column(nullable=false,length=2000) private String message;
    @Column(nullable=false,updatable=false) private Instant createdAt=Instant.now();
    protected AthleteFeedback(){}
    public AthleteFeedback(Long playerId,Long coachId,String message){this.playerId=playerId;this.coachId=coachId;this.message=message;}
    public Long getId(){return id;} public Long getPlayerId(){return playerId;} public Long getCoachId(){return coachId;}
    public String getMessage(){return message;} public Instant getCreatedAt(){return createdAt;}
}
