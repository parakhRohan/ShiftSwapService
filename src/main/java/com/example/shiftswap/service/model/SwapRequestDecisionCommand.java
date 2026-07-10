package com.example.shiftswap.service.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SwapRequestDecisionCommand(
        @NotNull Long swapRequestId,
        @NotNull Long decidingEmployeeId,
        @Size(max = 500) String note
) {
}
