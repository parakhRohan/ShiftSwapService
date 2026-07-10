package com.example.shiftswap.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.shiftswap.domain.exception.IllegalStateTransitionException;
import com.example.shiftswap.domain.exception.InvalidSwapRequestException;
import com.example.shiftswap.domain.exception.UnauthorizedActionException;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SwapRequestTest {

    private Employee alice;
    private Employee bob;
    private Employee manager;
    private Shift aliceShift;
    private Shift bobShift;

    @BeforeEach
    void setUp() {
        alice = new Employee("Alice", EmployeeRole.EMPLOYEE);
        bob = new Employee("Bob", EmployeeRole.EMPLOYEE);
        manager = new Employee("Manager", EmployeeRole.MANAGER);
        aliceShift = new Shift(alice, LocalDate.of(2026, 8, 1), LocalTime.of(9, 0), LocalTime.of(17, 0));
        bobShift = new Shift(bob, LocalDate.of(2026, 8, 1), LocalTime.of(13, 0), LocalTime.of(21, 0));
    }

    @Test
    void createsAPendingRequestWhenInputIsValid() {
        SwapRequest request = new SwapRequest(alice, aliceShift, bob, bobShift, "need to leave early");

        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.PENDING);
        assertThat(request.getRequester()).isEqualTo(alice);
        assertThat(request.getTargetEmployee()).isEqualTo(bob);
        assertThat(request.getDecidedAt()).isNull();
    }

    @Test
    void rejectsSwapWithSelf() {
        assertThatThrownBy(() -> new SwapRequest(alice, aliceShift, alice, bobShift, null))
                .isInstanceOf(InvalidSwapRequestException.class);
    }

    @Test
    void rejectsWhenRequesterShiftDoesNotBelongToRequester() {
        assertThatThrownBy(() -> new SwapRequest(bob, aliceShift, alice, bobShift, null))
                .isInstanceOf(InvalidSwapRequestException.class);
    }

    @Test
    void rejectsWhenTargetShiftDoesNotBelongToTargetEmployee() {
        assertThatThrownBy(() -> new SwapRequest(alice, aliceShift, bob, aliceShift, null))
                .isInstanceOf(InvalidSwapRequestException.class);
    }

    @Test
    void approveSwapsShiftOwnership() {
        SwapRequest request = new SwapRequest(alice, aliceShift, bob, bobShift, null);

        request.approve(manager, "looks fine");

        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.APPROVED);
        assertThat(aliceShift.getEmployee()).isEqualTo(bob);
        assertThat(bobShift.getEmployee()).isEqualTo(alice);
        assertThat(request.getDecidedBy()).isEqualTo(manager);
        assertThat(request.getDecidedAt()).isNotNull();
    }

    @Test
    void approveByNonManagerIsRejected() {
        SwapRequest request = new SwapRequest(alice, aliceShift, bob, bobShift, null);

        assertThatThrownBy(() -> request.approve(bob, null))
                .isInstanceOf(UnauthorizedActionException.class);
        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.PENDING);
        // shifts must not have moved
        assertThat(aliceShift.getEmployee()).isEqualTo(alice);
        assertThat(bobShift.getEmployee()).isEqualTo(bob);
    }

    @Test
    void approvingAlreadyApprovedRequestIsIdempotent() {
        SwapRequest request = new SwapRequest(alice, aliceShift, bob, bobShift, null);
        request.approve(manager, "first approval");

        // simulate a client retry of the same approve call
        request.approve(manager, "second approval attempt");

        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.APPROVED);
        // the second call must not re-swap shifts back
        assertThat(aliceShift.getEmployee()).isEqualTo(bob);
        assertThat(bobShift.getEmployee()).isEqualTo(alice);
        // decision note from the *first* approval is preserved, not overwritten
        assertThat(request.getDecisionNote()).isEqualTo("first approval");
    }

    @Test
    void approvingAnAlreadyRejectedRequestFails() {
        SwapRequest request = new SwapRequest(alice, aliceShift, bob, bobShift, null);
        request.reject(manager, "not approved");

        assertThatThrownBy(() -> request.approve(manager, null))
                .isInstanceOf(IllegalStateTransitionException.class);
        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.REJECTED);
        // shifts must remain with their original owners
        assertThat(aliceShift.getEmployee()).isEqualTo(alice);
        assertThat(bobShift.getEmployee()).isEqualTo(bob);
    }

    @Test
    void rejectingAnAlreadyApprovedRequestFails() {
        SwapRequest request = new SwapRequest(alice, aliceShift, bob, bobShift, null);
        request.approve(manager, "approved");

        assertThatThrownBy(() -> request.reject(manager, null))
                .isInstanceOf(IllegalStateTransitionException.class);
        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.APPROVED);
    }

    @Test
    void rejectingAlreadyRejectedRequestIsIdempotent() {
        SwapRequest request = new SwapRequest(alice, aliceShift, bob, bobShift, null);
        request.reject(manager, "first rejection");

        request.reject(manager, "second rejection attempt");

        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.REJECTED);
        assertThat(request.getDecisionNote()).isEqualTo("first rejection");
    }

    @Test
    void rejectByNonManagerIsRejected() {
        SwapRequest request = new SwapRequest(alice, aliceShift, bob, bobShift, null);

        assertThatThrownBy(() -> request.reject(alice, null))
                .isInstanceOf(UnauthorizedActionException.class);
        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.PENDING);
    }
}
