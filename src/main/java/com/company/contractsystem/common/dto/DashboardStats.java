package com.company.contractsystem.common.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class DashboardStats {
    private Map<String, Long> counters;
    private String role;
}
