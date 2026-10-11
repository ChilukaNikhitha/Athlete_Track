package com.athletetrack.wellness;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name="wellness_check_ins")
public class WellnessCheckIn {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long playerId;
    @Column(nullable=false) private LocalDate recordedAt;
    @Column(nullable=false) private Double sleepHours;
    @Column(nullable=false) private Integer recoveryScore;
    @Column(nullable=false) private Integer energyLevel;
    @Column(length=500) private String notes;
    protected WellnessCheckIn(){}
    public WellnessCheckIn(Long playerId,LocalDate date,Double sleep,Integer recovery,Integer energy,String notes){
        this.playerId=playerId;this.recordedAt=date;this.sleepHours=sleep;this.recoveryScore=recovery;this.energyLevel=energy;this.notes=notes;
    }
    public Long getId(){return id;} public Long getPlayerId(){return playerId;} public LocalDate getRecordedAt(){return recordedAt;}
    public Double getSleepHours(){return sleepHours;} public Integer getRecoveryScore(){return recoveryScore;}
    public Integer getEnergyLevel(){return energyLevel;} public String getNotes(){return notes;}
}
