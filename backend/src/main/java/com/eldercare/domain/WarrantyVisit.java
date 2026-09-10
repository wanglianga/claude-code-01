package com.eldercare.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "warranty_visit")
@Getter
@Setter
@NoArgsConstructor
public class WarrantyVisit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long applicationId;

    private LocalDateTime visitTime;
    /** SATISFIED / ISSUES */
    private String result;
    @Column(length = 1024)
    private String content;
    private LocalDate nextVisitDate;

    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
