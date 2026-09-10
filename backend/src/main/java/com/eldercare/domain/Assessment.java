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
    private String fallRiskLevel;
    @Column(length = 1024)
    private String summary;
    private LocalDateTime assessedAt;

    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
