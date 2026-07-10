package com.example.shiftswap.domain.exception;

public class EmployeeNotFoundException extends RuntimeException {
    public EmployeeNotFoundException(Long id) {
        super("Employee not found: " + id);
    }
}
