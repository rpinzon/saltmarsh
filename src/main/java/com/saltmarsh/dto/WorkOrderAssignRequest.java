package com.saltmarsh.dto;

import jakarta.validation.constraints.NotNull;

public record WorkOrderAssignRequest(@NotNull Long assigneeId) {}
