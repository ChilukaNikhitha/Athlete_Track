package com.athletetrack.profile;

import com.athletetrack.user.UserAccount;
import jakarta.persistence.*;

@Entity
@Table(name="player_profiles", uniqueConstraints=@UniqueConstraint(columnNames="user_id"))
public class PlayerProfile {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id",nullable=false) private UserAccount user;
    private Integer age;
    private Double heightCm;
    private Double weightKg;
    @Column(length=80) private String sport;
    @Column(length=500) private String goal;
    protected PlayerProfile() {}
    public PlayerProfile(UserAccount user) { this.user=user; }
    public Long getId(){return id;} public UserAccount getUser(){return user;} public Integer getAge(){return age;}
    public Double getHeightCm(){return heightCm;} public Double getWeightKg(){return weightKg;}
    public String getSport(){return sport;} public String getGoal(){return goal;}
    public void update(Integer age,Double height,Double weight,String sport,String goal) {
        this.age=age;this.heightCm=height;this.weightKg=weight;this.sport=sport;this.goal=goal;
    }
}
