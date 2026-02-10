package com.company.contractsystem.approval.entity;

import com.company.contractsystem.user.entity.User;
import jakarta.persistence.*;

@Entity
@Table(name = "approval_mappings")
public class ApprovalMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Legal user who creates contracts
    @ManyToOne
    @JoinColumn(name = "legal_user_id", nullable = false)
    private User legalUser;

    // Finance reviewer assigned to this legal user
    @ManyToOne
    @JoinColumn(name = "finance_user_id", nullable = false)
    private User financeUser;

    // Client assigned to this finance reviewer
    @ManyToOne
    @JoinColumn(name = "client_user_id", nullable = false)
    private User clientUser;

    public Long getId() {
        return id;
    }

    public User getLegalUser() {
        return legalUser;
    }

    public void setLegalUser(User legalUser) {
        this.legalUser = legalUser;
    }

    public User getFinanceUser() {
        return financeUser;
    }

    public void setFinanceUser(User financeUser) {
        this.financeUser = financeUser;
    }

    public User getClientUser() {
        return clientUser;
    }

    public void setClientUser(User clientUser) {
        this.clientUser = clientUser;
    }
}
