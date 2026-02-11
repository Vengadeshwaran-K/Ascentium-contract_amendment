package com.company.contractsystem.contract.entity;

import com.company.contractsystem.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "contracts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String contractName;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    private LocalDate effectiveDate;

    private BigDecimal contractAmount;

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private ContractStatus status;
}
