package com.athletetrack.performance;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name="performance_entries")
public class PerformanceEntry {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long playerId;
    @Column(nullable=false,length=80) private String metric;
    @Column(name="metric_value",nullable=false) private Double value;
    @Column(nullable=false,length=30) private String unit;
    @Column(nullable=false) private LocalDate recordedAt;
    @Column(length=500) private String notes;
    protected PerformanceEntry(){}
    public PerformanceEntry(Long playerId,String metric,Double value,String unit,LocalDate date,String notes){
        this.playerId=playerId;this.metric=metric;this.value=value;this.unit=unit;this.recordedAt=date;this.notes=notes;
    }
    public Long getId(){return id;} public Long getPlayerId(){return playerId;} public String getMetric(){return metric;}
    public Double getValue(){return value;} public String getUnit(){return unit;} public LocalDate getRecordedAt(){return recordedAt;} public String getNotes(){return notes;}
}
