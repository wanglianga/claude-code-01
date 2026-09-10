package com.eldercare.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_log")
@Getter
@Setter
@NoArgsConstructor
public class WorkflowLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long applicationId;

    @Column(nullable = false)
    private String action;

    private String fromStatus;
    private String toStatus;
    private Long operatorId;
    private String operatorName;
    @Column(length = 512)
    private String note;

    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
