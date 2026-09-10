package com.eldercare.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 现场施工变更。流转：
 * SUBMITTED(待家属确认) -> FAMILY_CONFIRMED(待社区复核)
 *   -> COMMUNITY_APPROVED(核准，重算费用并回到施工)
 *   / REJECTED(未通过，保留原方案，回到施工)
 *   / COORDINATING(转社区协调，暂挂)
 * 家属也可驳回（REJECTED 回到施工，按原方案执行）。
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

    /** WALL_UNDRILLABLE / TILE_CRACK / PIPE_BLOCK / HOSPITAL / FAMILY_CHANGE / MODEL_MISMATCH / ADD_ITEM */
    @Column(nullable = false)
    private String reasonType;

    @Column(nullable = false, length = 512)
    private String description;

    /** 变更总费用变化（= 材料变化 + 人工变化，提交时由明细汇总） */
    @Column(nullable = false)
    private BigDecimal costDelta = BigDecimal.ZERO;

    /** 变更费用拆分 */
    private BigDecimal materialFeeDelta = BigDecimal.ZERO;
    private BigDecimal laborFeeDelta = BigDecimal.ZERO;

    /** 现场照片（档案编号/说明，演示环境以文字代替文件上传） */
    @Column(length = 1024)
    private String sitePhotos;
    /** 新的材料需求 */
    @Column(length = 1024)
    private String materialRequirements;

    /** 社区判断是否影响补贴资格 */
    private Boolean subsidyAffected;
    /** 社区决策: APPROVED / REJECTED / COORDINATING */
    private String communityDecision;
    @Column(length = 512)
    private String coordinationNote;
    private LocalDateTime resolvedAt;

    /** 核准后重算的费用快照（全单口径） */
    private BigDecimal newMaterialFee;
    private BigDecimal newLaborFee;
    private BigDecimal newSubsidyAmount;
    private BigDecimal newTotalCost;
    /** 本次变更新增的可报销金额 */
    private BigDecimal reimbursementDelta;

    /** SUBMITTED / FAMILY_CONFIRMED / COMMUNITY_APPROVED / REJECTED / COORDINATING */
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

    /** 变更项目明细（提交时随表单传入，不持久化在本表，存 change_item） */
    @Transient
    private java.util.List<ChangeItem> items;
}
