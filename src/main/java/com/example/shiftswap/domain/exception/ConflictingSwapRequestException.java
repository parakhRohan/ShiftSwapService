package com.example.shiftswap.domain.exception;


public class ConflictingSwapRequestException extends RuntimeException {
    public ConflictingSwapRequestException(Long shiftId) {
        super("Shift " + shiftId + " already has a pending swap request");
    }
}
