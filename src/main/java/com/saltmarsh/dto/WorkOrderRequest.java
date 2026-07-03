package com.saltmarsh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

public record WorkOrderRequest(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 2000) String description,
        @NotBlank @Pattern(regexp = "LOW|MEDIUM|HIGH|URGENT") String priority,
        @NotBlank @Pattern(regexp = "ELECTRICAL|PLUMBING|HULL|ENGINE|DOCK|OTHER") String category,
        Long vesselId,
        Long berthId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dueAt
) {}
