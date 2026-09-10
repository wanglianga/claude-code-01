package com.eldercare.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "construction_schedule")
@Getter
@Setter
@NoArgsConstructor
public class ConstructionSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long applicationId;

    private Long teamId;
    private LocalDate scheduledStart;
    private LocalDate scheduledEnd;
    private String buildingAccess;
    private String elevatorPlan;
    private String materialArrival;
    private String elderSchedule;
    private String noiseRestriction;
    @Column(length = 512)
    private String remark;

    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
