package com.example.shiftswap.domain.model;

import com.example.shiftswap.domain.exception.IllegalStateTransitionException;
import com.example.shiftswap.domain.exception.InvalidSwapRequestException;
import com.example.shiftswap.domain.exception.UnauthorizedActionException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "swap_requests")
public class SwapRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private Employee requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_shift_id", nullable = false)
    private Shift requesterShift;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_employee_id", nullable = false)
    private Employee targetEmployee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_shift_id", nullable = false)
    private Shift targetShift;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SwapRequestStatus status;

    @Column(length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_id")
    private Employee decidedBy;

    @Column(length = 500)
    private String decisionNote;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant decidedAt;

    protected SwapRequest() {
    }

    public SwapRequest(Employee requester, Shift requesterShift, Employee targetEmployee,
                        Shift targetShift, String reason) {
        this.requester = Objects.requireNonNull(requester, "requester must not be null");
        this.requesterShift = Objects.requireNonNull(requesterShift, "requesterShift must not be null");
        this.targetEmployee = Objects.requireNonNull(targetEmployee, "targetEmployee must not be null");
        this.targetShift = Objects.requireNonNull(targetShift, "targetShift must not be null");
        this.reason = reason;
        this.status = SwapRequestStatus.PENDING;
        this.createdAt = Instant.now();
        validateInvariants();
    }

    private void validateInvariants() {
        if (requester.equals(targetEmployee)) {
            throw new InvalidSwapRequestException("An employee cannot request a swap with themselves");
        }
        if (requesterShift.equals(targetShift)) {
            throw new InvalidSwapRequestException("A shift cannot be swapped with itself");
        }
        if (!requesterShift.getEmployee().equals(requester)) {
            throw new InvalidSwapRequestException(
                    "Requester shift %d does not belong to requester %d"
                            .formatted(requesterShift.getId(), requester.getId()));
        }
        if (!targetShift.getEmployee().equals(targetEmployee)) {
            throw new InvalidSwapRequestException(
                    "Target shift %d does not belong to target employee %d"
                            .formatted(targetShift.getId(), targetEmployee.getId()));
        }
    }

    public void approve(Employee decider, String note) {
        requireManager(decider);
        if (status == SwapRequestStatus.APPROVED) {
            return;
        }
        if (status != SwapRequestStatus.PENDING) {
            throw new IllegalStateTransitionException(status, "approve");
        }
        Employee originalRequesterOwner = requesterShift.getEmployee();
        Employee originalTargetOwner = targetShift.getEmployee();
        requesterShift.reassignTo(originalTargetOwner);
        targetShift.reassignTo(originalRequesterOwner);

        this.status = SwapRequestStatus.APPROVED;
        this.decidedBy = decider;
        this.decisionNote = note;
        this.decidedAt = Instant.now();
    }

    public void reject(Employee decider, String note) {
        requireManager(decider);
        if (status == SwapRequestStatus.REJECTED) {
            return;
        }
        if (status != SwapRequestStatus.PENDING) {
            throw new IllegalStateTransitionException(status, "reject");
        }
        this.status = SwapRequestStatus.REJECTED;
        this.decidedBy = decider;
        this.decisionNote = note;
        this.decidedAt = Instant.now();
    }

    private void requireManager(Employee decider) {
        Objects.requireNonNull(decider, "decider must not be null");
        if (!decider.isManager()) {
            throw new UnauthorizedActionException(
                    "Employee %d is not authorized to decide swap requests".formatted(decider.getId()));
        }
    }

    public Long getId() {
        return id;
    }

    public Employee getRequester() {
        return requester;
    }

    public Shift getRequesterShift() {
        return requesterShift;
    }

    public Employee getTargetEmployee() {
        return targetEmployee;
    }

    public Shift getTargetShift() {
        return targetShift;
    }

    public SwapRequestStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public Employee getDecidedBy() {
        return decidedBy;
    }

    public String getDecisionNote() {
        return decisionNote;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SwapRequest other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "SwapRequest{id=%d, status=%s, requesterShift=%d, targetShift=%d}"
                .formatted(id, status, requesterShift.getId(), targetShift.getId());
    }
}
