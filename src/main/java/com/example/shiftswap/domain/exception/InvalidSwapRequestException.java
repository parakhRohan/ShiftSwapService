package com.example.shiftswap.domain.exception;

public class InvalidSwapRequestException extends RuntimeException {
    public InvalidSwapRequestException(String message) {
        super(message);
    }
}
