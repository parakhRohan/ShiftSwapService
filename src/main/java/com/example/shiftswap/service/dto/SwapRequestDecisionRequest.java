package com.example.shiftswap.service.model;

import jakarta.validation.constraints.Size;

public record SwapRequestDecisionRequest(
        @Size(max = 500) String note
) {
}
