package com.example.shiftswap.domain.exception;

public class SwapRequestNotFoundException extends RuntimeException {
    public SwapRequestNotFoundException(Long id) {
        super("Swap request not found: " + id);
    }
}
