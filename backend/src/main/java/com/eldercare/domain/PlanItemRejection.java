package com.eldercare.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 家属对高风险改造项目的拒绝留痕。
 * 家属删除方案项目（尤其高风险相关项）时，必须填写拒绝原因，
 * 系统同时保留评估师的建议说明；街道补贴审核时可查看风险判断如何影响最终方案。
 */
@Entity
@Table(name = "plan_item_rejection")
@Getter
@Setter
@NoArgsConstructor
public class PlanItemRejection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long applicationId;

    private Long planItemId;

    @Column(nullable = false)
    private String itemName;

    private String category;

    @Column(nullable = false, length = 512)
    private String assessorNote;

    @Column(nullable = false, length = 512)
    private String familyReason;

    private String riskLevel;

    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
