package com.eldercare.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "assessment")
@Getter
@Setter
@NoArgsConstructor
public class Assessment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long applicationId;

    private Long assessorId;
    private BigDecimal thresholdHeight;
    private BigDecimal bathroomWidth;
    private BigDecimal bathroomDepth;
    private String wallMaterial;
    private String nightLighting;
    private String bedTransferDifficulty;
    private String trialActions;

    /** 跌倒风险等级: 高/中/低（平台依据五维度评分生成） */
    private String fallRiskLevel;

    @Column(length = 1024)
    private String summary;

    // ---------- 风险分级五维度 ----------
    /** 现场记录的老人行动能力（独立/拄拐/搀扶/轮椅/卧床），与申请时自评分开 */
    private String mobilityObserved;
    private Integer mobilityScore;

    /** 卫生间湿滑程度: 干燥/一般/较湿/积水 */
    private String wetness;
    private Integer wetnessScore;

    /** 床边起身难度 0-3 */
    private Integer bedDifficultyScore;

    /** 夜间照明评分 0-2 */
    private Integer lightingScore;

    /** 紧急呼叫条件（同住人、是否有手机、离邻居距离等） */
    private String emergencyCondition;
    private Integer emergencyScore;

    /** 平台综合评分 */
    private Integer riskScore;
    /** 风险因子明细（平台生成） */
    @Column(length = 1024)
    private String riskFactors;
    /** 高风险：施工陪同/临时照护建议 */
    @Column(length = 512)
    private String careRecommendation;

    private LocalDateTime assessedAt;

    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
