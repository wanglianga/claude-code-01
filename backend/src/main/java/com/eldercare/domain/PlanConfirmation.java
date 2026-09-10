package com.eldercare.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "plan_confirmation")
@Getter
@Setter
@NoArgsConstructor
public class PlanConfirmation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long applicationId;

    private Integer roundNo = 1;

    @Column(nullable = false)
    private BigDecimal totalCost;
    @Column(nullable = false)
    private BigDecimal subsidyAmount;
    @Column(nullable = false)
    private BigDecimal selfPay;

    private Boolean familyConfirmed;
    private LocalDateTime familyConfirmedAt;
    private String familySigner;

    private Boolean communityApproved;
    private String communityRemark;
    private LocalDateTime communityReviewedAt;

    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
