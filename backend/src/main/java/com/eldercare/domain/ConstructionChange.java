package com.eldercare.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 现场施工变更。流转：
 * SUBMITTED(待家属确认) -> FAMILY_CONFIRMED(待社区复核) -> COMMUNITY_APPROVED(回到施工)
 * 任一环节 REJECTED 则退回施工队处理。
 */
@Entity
@Table(name = "construction_change")
@Getter
@Setter
@NoArgsConstructor
public class ConstructionChange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long applicationId;

    /** WALL_UNDRILLABLE / PIPE_BLOCK / HOSPITAL / FAMILY_CHANGE / MODEL_MISMATCH */
    @Column(nullable = false)
    private String reasonType;

    @Column(nullable = false, length = 512)
    private String description;

    @Column(nullable = false)
    private BigDecimal costDelta = BigDecimal.ZERO;

    /** SUBMITTED / FAMILY_CONFIRMED / COMMUNITY_APPROVED / REJECTED */
    @Column(nullable = false)
    private String status = "SUBMITTED";

    private LocalDateTime familyConfirmedAt;
    @Column(length = 512)
    private String familyOpinion;
    @Column(length = 512)
    private String communityRemark;
    private LocalDateTime communityReviewedAt;

    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
