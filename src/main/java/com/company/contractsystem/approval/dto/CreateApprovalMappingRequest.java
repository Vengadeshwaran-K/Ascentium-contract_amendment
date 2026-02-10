package com.company.contractsystem.approval.dto;

import jakarta.validation.constraints.NotNull;

public class CreateApprovalMappingRequest {

    @NotNull
    private Long legalUserId;

    @NotNull
    private Long financeUserId;

    @NotNull
    private Long clientUserId;

    public Long getLegalUserId() {
        return legalUserId;
    }

    public void setLegalUserId(Long legalUserId) {
        this.legalUserId = legalUserId;
    }

    public Long getFinanceUserId() {
        return financeUserId;
    }

    public void setFinanceUserId(Long financeUserId) {
        this.financeUserId = financeUserId;
    }

    public Long getClientUserId() {
        return clientUserId;
    }

    public void setClientUserId(Long clientUserId) {
        this.clientUserId = clientUserId;
    }
}
