package com.eldercare.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 适老化改造申请。状态机：
 * SUBMITTED(待核验) -> VERIFIED(已核验待派单) -> ASSIGNED(已派单待评估)
 * -> ASSESSED(已评估待方案) -> PLAN_REVIEW(方案待家属确认)
 * -> PLAN_FAMILY_CONFIRMED(家属已确认待社区复核) -> PLAN_APPROVED(方案通过待接单)
 * -> SCHEDULED(已排期待施工) -> IN_CONSTRUCTION(施工中)
 * -> CHANGE_PENDING_FAMILY(变更待家属确认) / CHANGE_PENDING_COMMUNITY(变更待社区复核)
 * -> COMPLETED(竣工待街道审核) -> SUBSIDY_REVIEWED(补贴已审核) -> SETTLED(已结算)
 * -> VISITED(已质保回访, 终态)
 */
@Entity
@Table(name = "application")
@Getter
@Setter
@NoArgsConstructor
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String applicantName;
    private String elderName;
    private Integer elderAge;
    private String idCard;
    private String phone;
    private String address;
    private String community;
    private Integer floor;
    private Boolean hasElevator = false;
    private Boolean livingAlone = false;
    private String mobility;
    private String fallHistory;
    private String bathroomStatus;
    private String bedroomStatus;
    private String houseOwnership;
    private String expectedItems;

    @Column(nullable = false)
    private String status = "SUBMITTED";

    private Boolean subsidyEligible;
    private String verifyRemark;

    private Long assessorId;
    private Long applicantUserId;

    @Column(insertable = false, updatable = false)
    private LocalDateTime submittedAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime assignedAt;

    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    /** 非持久化：列表查询时附带评估风险等级（高/中/低），用于高风险优先标识 */
    @Transient
    private String riskLevel;

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }
}
