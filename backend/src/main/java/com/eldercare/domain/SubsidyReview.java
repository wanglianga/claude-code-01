package com.eldercare.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "subsidy_review")
@Getter
@Setter
@NoArgsConstructor
public class SubsidyReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long applicationId;

    private Long reviewerId;
    private BigDecimal approvedSubsidy;
    private BigDecimal selfPay;
    /** APPROVED / REJECTED */
    private String conclusion;
    @Column(length = 512)
    private String remark;
    private LocalDateTime reviewedAt;
}
