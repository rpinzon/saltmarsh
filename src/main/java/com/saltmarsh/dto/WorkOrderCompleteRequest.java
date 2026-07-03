package com.saltmarsh.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WorkOrderCompleteRequest(
        @NotNull @DecimalMin("0.00") BigDecimal laborHours,
        @DecimalMin("0.00") BigDecimal partsCost
) {}
