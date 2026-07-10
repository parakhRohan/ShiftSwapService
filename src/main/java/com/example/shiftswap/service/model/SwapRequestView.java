package com.example.shiftswap.service.model;

import com.example.shiftswap.domain.model.SwapRequest;
import com.example.shiftswap.domain.model.SwapRequestStatus;
import java.time.Instant;

public record SwapRequestView(
        Long id,
        Long requesterId,
        String requesterName,
        Long requesterShiftId,
        Long targetEmployeeId,
        String targetEmployeeName,
        Long targetShiftId,
        SwapRequestStatus status,
        String reason,
        Long decidedById,
        String decisionNote,
        Instant createdAt,
        Instant decidedAt
) {
    public static SwapRequestView from(SwapRequest swapRequest) {
        return new SwapRequestView(
                swapRequest.getId(),
                swapRequest.getRequester().getId(),
                swapRequest.getRequester().getName(),
                swapRequest.getRequesterShift().getId(),
                swapRequest.getTargetEmployee().getId(),
                swapRequest.getTargetEmployee().getName(),
                swapRequest.getTargetShift().getId(),
                swapRequest.getStatus(),
                swapRequest.getReason(),
                swapRequest.getDecidedBy() != null ? swapRequest.getDecidedBy().getId() : null,
                swapRequest.getDecisionNote(),
                swapRequest.getCreatedAt(),
                swapRequest.getDecidedAt()
        );
    }
}
