package com.eldercare.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlement")
@Getter
@Setter
@NoArgsConstructor
public class Settlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long applicationId;

    private BigDecimal totalAmount;
    private BigDecimal subsidyAmount;
    private BigDecimal familyPayAmount;
    private BigDecimal teamPayAmount;
    /** PENDING / SETTLED */
    @Column(nullable = false)
    private String status = "PENDING";
    private LocalDateTime settledAt;
}
