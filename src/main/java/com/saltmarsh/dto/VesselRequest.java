package com.saltmarsh.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record VesselRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 64) String registrationNumber,
        @NotNull @DecimalMin(value = "0.01") BigDecimal lengthFeet,
        @NotNull @DecimalMin(value = "0.01") BigDecimal beamFeet,
        @NotNull @DecimalMin(value = "0.01") BigDecimal draftFeet,
        @NotBlank @Pattern(regexp = "SAIL|POWER|CATAMARAN|OTHER") String vesselType,
        Long ownerId
) {}
