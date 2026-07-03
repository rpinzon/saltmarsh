package com.saltmarsh.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record BerthRequest(
        @NotBlank @Size(max = 16) String code,
        @NotBlank @Size(max = 32) String pier,
        @NotNull @DecimalMin(value = "0.01") BigDecimal maxLengthFeet,
        @NotNull @DecimalMin(value = "0.01") BigDecimal maxDraftFeet,
        @NotBlank @Pattern(regexp = "TRANSIENT|SEASONAL|LIVEABOARD") String berthType,
        @NotNull @DecimalMin(value = "0.00") BigDecimal dailyRate,
        @NotBlank @Pattern(regexp = "AVAILABLE|MAINTENANCE|OUT_OF_SERVICE") String status,
        @Size(max = 500) String notes
) {}
