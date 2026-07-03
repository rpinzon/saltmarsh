package com.saltmarsh.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WaitlistRequest(
        @NotNull Long vesselId,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate preferredStart,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate preferredEnd,
        BigDecimal minLengthFeet,
        @Size(max = 500) String notes
) {}
