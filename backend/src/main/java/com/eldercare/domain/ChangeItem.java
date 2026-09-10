package com.eldercare.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 变更涉及的项目清单（新增项目 / 材料替代或工艺变更）。
 * 社区核定时逐项标记 subsidyEligible（是否计入可报销范围）。
 */
@Entity
@Table(name = "change_item")
@Getter
@Setter
@NoArgsConstructor
public class ChangeItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long changeId;

    @Column(nullable = false)
    private Long applicationId;

    /** ADD 新增项目 / REPLACE 材料替代·工艺变更 */
    @Column(nullable = false)
    private String itemType;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String name;

    private String spec;
    private String unit;
    private Integer quantity = 1;

    @Column(nullable = false)
    private BigDecimal materialFee = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal laborFee = BigDecimal.ZERO;

    @Column(length = 512)
    private String reason;

    @Column(nullable = false)
    private Boolean subsidyEligible = false;

    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
