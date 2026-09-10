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

    /** 高风险家庭优先排期：1=优先，0=普通 */
    @Column(nullable = false)
    private Integer priority = 0;

    /** 施工期间照护安排: NONE / COMPANION(家属陪同) / TEMP_CARE(临时照护) */
    private String careRequired;
    @Column(length = 512)
    private String careArrangement;

    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
