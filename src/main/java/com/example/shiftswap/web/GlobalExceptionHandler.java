package com.example.shiftswap.web;

import com.example.shiftswap.domain.exception.ConflictingSwapRequestException;
import com.example.shiftswap.domain.exception.EmployeeNotFoundException;
import com.example.shiftswap.domain.exception.IllegalStateTransitionException;
import com.example.shiftswap.domain.exception.InvalidSwapRequestException;
import com.example.shiftswap.domain.exception.ShiftNotFoundException;
import com.example.shiftswap.domain.exception.SwapRequestNotFoundException;
import com.example.shiftswap.domain.exception.UnauthorizedActionException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({EmployeeNotFoundException.class, ShiftNotFoundException.class,
            SwapRequestNotFoundException.class})
    public ResponseEntity<ApiError> handleNotFound(RuntimeException ex, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, ex, request);
    }

    @ExceptionHandler(InvalidSwapRequestException.class)
    public ResponseEntity<ApiError> handleInvalidRequest(InvalidSwapRequestException ex,
                                                          HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, ex, request);
    }

    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedActionException ex,
                                                        HttpServletRequest request) {
        return respond(HttpStatus.FORBIDDEN, ex, request);
    }

    @ExceptionHandler({IllegalStateTransitionException.class, ConflictingSwapRequestException.class})
    public ResponseEntity<ApiError> handleConflict(RuntimeException ex, HttpServletRequest request) {
        return respond(HttpStatus.CONFLICT, ex, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                      HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                request.getRequestURI()));
    }

    private ResponseEntity<ApiError> respond(HttpStatus status, RuntimeException ex, HttpServletRequest request) {
        return ResponseEntity.status(status).body(ApiError.of(
                status.value(), status.getReasonPhrase(), ex.getMessage(), request.getRequestURI()));
    }
}
