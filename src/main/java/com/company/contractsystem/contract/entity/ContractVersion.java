package com.company.contractsystem.contract.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContractVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Enumerated(EnumType.STRING)
    private ContractStatus status;

    private int versionNumber;

    private String remarks;

    @ManyToOne
    @JoinColumn(name = "creator_id")
    private com.company.contractsystem.user.entity.User creator;

    private java.time.LocalDateTime updatedAt;

    private boolean active;
}
