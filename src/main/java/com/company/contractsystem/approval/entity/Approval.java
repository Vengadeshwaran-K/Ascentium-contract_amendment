package com.company.contractsystem.approval.entity;

import com.company.contractsystem.enums.ApprovalDecision;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Approval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long contractId;

    @Enumerated(EnumType.STRING)
    private ApprovalDecision decision;

    private String remarks;

    private String reviewerRole;

    private LocalDateTime reviewedAt;
}
