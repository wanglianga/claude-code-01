package com.eldercare.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "completion")
@Getter
@Setter
@NoArgsConstructor
public class Completion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long applicationId;

    @Column(length = 1024)
    private String beforePhotos;
    @Column(length = 1024)
    private String afterPhotos;
    @Column(length = 1024)
    private String trialRecord;
    private String familySigner;
    private LocalDateTime signedAt;
    @Column(length = 1024)
    private String materialsDetail;
    @Column(length = 1024)
    private String costDetail;
    private BigDecimal totalCost;
    private LocalDateTime completedAt;
}
