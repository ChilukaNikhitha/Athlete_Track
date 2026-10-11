package com.athletetrack.plan;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name="wellness_plans")
public class WellnessPlan {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long playerId;
    @Column(nullable=false) private Long coachId;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private PlanType type;
    @Column(nullable=false,length=120) private String title;
    @Column(nullable=false,length=2000) private String details;
    private LocalDate scheduledFor;
    @Column(nullable=false,updatable=false) private Instant createdAt=Instant.now();
    protected WellnessPlan(){}
    public WellnessPlan(Long playerId,Long coachId,PlanType type,String title,String details,LocalDate scheduledFor){
        this.playerId=playerId;this.coachId=coachId;this.type=type;this.title=title;this.details=details;this.scheduledFor=scheduledFor;
    }
    public Long getId(){return id;} public Long getPlayerId(){return playerId;} public Long getCoachId(){return coachId;}
    public PlanType getType(){return type;} public String getTitle(){return title;} public String getDetails(){return details;}
    public LocalDate getScheduledFor(){return scheduledFor;} public Instant getCreatedAt(){return createdAt;}
}
