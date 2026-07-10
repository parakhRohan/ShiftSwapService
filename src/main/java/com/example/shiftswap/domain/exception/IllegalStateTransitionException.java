package com.example.shiftswap.domain.exception;

import com.example.shiftswap.domain.model.SwapRequestStatus;

public class IllegalStateTransitionException extends RuntimeException {
    public IllegalStateTransitionException(SwapRequestStatus current, String attemptedAction) {
        super("Cannot %s a swap request in status %s".formatted(attemptedAction, current));
    }
}
