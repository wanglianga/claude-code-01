package com.eldercare.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "plan_item")
@Getter
@Setter
@NoArgsConstructor
public class PlanItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long applicationId;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 512)
    private String reason;

    private String spec;
    private String unit;
    private Integer quantity = 1;

    @Column(nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private BigDecimal subsidyCap;

    private String constructionImpact;

    /** PROPOSED / ACCEPTED / REMOVED / ADDED */
    @Column(nullable = false)
    private String status = "PROPOSED";

    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
