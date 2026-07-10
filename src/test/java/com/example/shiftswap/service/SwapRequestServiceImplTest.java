package com.example.shiftswap.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.shiftswap.domain.exception.ConflictingSwapRequestException;
import com.example.shiftswap.domain.exception.EmployeeNotFoundException;
import com.example.shiftswap.domain.exception.SwapRequestNotFoundException;
import com.example.shiftswap.domain.model.Employee;
import com.example.shiftswap.domain.model.EmployeeRole;
import com.example.shiftswap.domain.model.Shift;
import com.example.shiftswap.domain.model.SwapRequest;
import com.example.shiftswap.domain.repository.EmployeeRepository;
import com.example.shiftswap.domain.repository.ShiftRepository;
import com.example.shiftswap.domain.repository.SwapRequestRepository;
import com.example.shiftswap.service.model.CreateSwapRequestCommand;
import com.example.shiftswap.service.model.SwapRequestDecisionCommand;
import com.example.shiftswap.service.model.SwapRequestView;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SwapRequestServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private ShiftRepository shiftRepository;
    @Mock
    private SwapRequestRepository swapRequestRepository;

    private SwapRequestServiceImpl service;

    private Employee alice;
    private Employee bob;
    private Employee manager;
    private Shift aliceShift;
    private Shift bobShift;

    @BeforeEach
    void setUp() {
        service = new SwapRequestServiceImpl(employeeRepository, shiftRepository, swapRequestRepository);

        alice = withId(new Employee("Alice", EmployeeRole.EMPLOYEE), 1L);
        bob = withId(new Employee("Bob", EmployeeRole.EMPLOYEE), 2L);
        manager = withId(new Employee("Manager", EmployeeRole.MANAGER), 3L);
        aliceShift = withId(new Shift(alice, LocalDate.of(2026, 8, 1), LocalTime.of(9, 0), LocalTime.of(17, 0)), 10L);
        bobShift = withId(new Shift(bob, LocalDate.of(2026, 8, 1), LocalTime.of(13, 0), LocalTime.of(21, 0)), 11L);
    }

    @Test
    void createsSwapRequestWhenActingEmployeeIsTheRequester() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(shiftRepository.findById(10L)).thenReturn(Optional.of(aliceShift));
        when(shiftRepository.findById(11L)).thenReturn(Optional.of(bobShift));
        when(swapRequestRepository.existsByStatusAndRequesterShiftIdOrStatusAndTargetShiftId(
                any(), any(), any(), any())).thenReturn(false);
        when(swapRequestRepository.save(any(SwapRequest.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), 100L));

        CreateSwapRequestCommand command = new CreateSwapRequestCommand(1L, 10L, 2L, 11L, "please");
        SwapRequestView result = service.createSwapRequest(command);

        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.requesterId()).isEqualTo(1L);
        assertThat(result.targetEmployeeId()).isEqualTo(2L);
    }

    @Test
    void rejectsCreationWhenActingEmployeeIsNotTheRequester() {
        CreateSwapRequestCommand command = new CreateSwapRequestCommand(2L, 10L, 2L, 11L, null);

        assertThatThrownBy(() -> service.createSwapRequest(command))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void rejectsCreationWhenRequesterShiftAlreadyHasAPendingRequest() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(shiftRepository.findById(10L)).thenReturn(Optional.of(aliceShift));
        when(shiftRepository.findById(11L)).thenReturn(Optional.of(bobShift));
        when(swapRequestRepository.existsByStatusAndRequesterShiftIdOrStatusAndTargetShiftId(
                any(), any(), any(), any())).thenReturn(true);

        CreateSwapRequestCommand command = new CreateSwapRequestCommand(1L, 10L, 2L, 11L, null);

        assertThatThrownBy(() -> service.createSwapRequest(command))
                .isInstanceOf(ConflictingSwapRequestException.class);
    }

    @Test
    void rejectsCreationWhenRequesterEmployeeDoesNotExist() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        CreateSwapRequestCommand command = new CreateSwapRequestCommand(1L, 10L, 2L, 11L, null);

        assertThatThrownBy(() -> service.createSwapRequest(command))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void approveDelegatesToDomainAndPersistsResult() {
        SwapRequest swapRequest = withId(new SwapRequest(alice, aliceShift, bob, bobShift, null), 100L);
        when(swapRequestRepository.findById(100L)).thenReturn(Optional.of(swapRequest));
        when(employeeRepository.findById(3L)).thenReturn(Optional.of(manager));
        when(swapRequestRepository.save(swapRequest)).thenReturn(swapRequest);

        SwapRequestDecisionCommand command = new SwapRequestDecisionCommand(100L, 3L, "ok");
        SwapRequestView result = service.approve(command);

        assertThat(result.status().name()).isEqualTo("APPROVED");
        assertThat(aliceShift.getEmployee()).isEqualTo(bob);
        assertThat(bobShift.getEmployee()).isEqualTo(alice);
        verify(swapRequestRepository).save(swapRequest);
    }

    @Test
    void approveOnMissingSwapRequestThrowsNotFound() {
        when(swapRequestRepository.findById(999L)).thenReturn(Optional.empty());

        SwapRequestDecisionCommand command = new SwapRequestDecisionCommand(999L, 3L, null);

        assertThatThrownBy(() -> service.approve(command))
                .isInstanceOf(SwapRequestNotFoundException.class);
    }

    /**
     * Test helper: JPA entities normally receive their id from the
     * persistence provider on insert. In pure unit tests (no persistence
     * context), reflection is the least invasive way to simulate "this
     * entity has already been saved" without adding test-only setters to
     * production classes.
     */
    private static <T> T withId(T entity, Long id) {
        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
            return entity;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to set id via reflection for test setup", e);
        }
    }
}
