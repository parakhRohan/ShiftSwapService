package com.example.shiftswap.service.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSwapRequestCommand(
        @NotNull Long actingEmployeeId,
        @NotNull Long requesterShiftId,
        @NotNull Long targetEmployeeId,
        @NotNull Long targetShiftId,
        @Size(max = 500) String reason
) {
}
