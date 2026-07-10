package com.example.shiftswap.service;

import com.example.shiftswap.domain.exception.ConflictingSwapRequestException;
import com.example.shiftswap.domain.exception.EmployeeNotFoundException;
import com.example.shiftswap.domain.exception.ShiftNotFoundException;
import com.example.shiftswap.domain.exception.SwapRequestNotFoundException;
import com.example.shiftswap.domain.exception.UnauthorizedActionException;
import com.example.shiftswap.domain.model.Employee;
import com.example.shiftswap.domain.model.Shift;
import com.example.shiftswap.domain.model.SwapRequest;
import com.example.shiftswap.domain.model.SwapRequestStatus;
import com.example.shiftswap.domain.repository.EmployeeRepository;
import com.example.shiftswap.domain.repository.ShiftRepository;
import com.example.shiftswap.domain.repository.SwapRequestRepository;
import com.example.shiftswap.service.model.CreateSwapRequestCommand;
import com.example.shiftswap.service.model.SwapRequestDecisionCommand;
import com.example.shiftswap.service.model.SwapRequestView;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SwapRequestServiceImpl implements SwapRequestService {

    private final EmployeeRepository employeeRepository;
    private final ShiftRepository shiftRepository;
    private final SwapRequestRepository swapRequestRepository;

    public SwapRequestServiceImpl(EmployeeRepository employeeRepository,
                                   ShiftRepository shiftRepository,
                                   SwapRequestRepository swapRequestRepository) {
        this.employeeRepository = employeeRepository;
        this.shiftRepository = shiftRepository;
        this.swapRequestRepository = swapRequestRepository;
    }

    @Override
    @Transactional
    public SwapRequestView createSwapRequest(CreateSwapRequestCommand command) {
        Employee requester = getEmployeeOrThrow(command.actingEmployeeId());
        Employee targetEmployee = getEmployeeOrThrow(command.targetEmployeeId());
        Shift requesterShift = getShiftOrThrow(command.requesterShiftId());
        Shift targetShift = getShiftOrThrow(command.targetShiftId());

        if (!requesterShift.getEmployee().getId().equals(requester.getId())) {
            throw new UnauthorizedActionException(
                    "Employee %d cannot create a swap request for shift %d"
                            .formatted(requester.getId(), requesterShift.getId()));
        }

        if (swapRequestRepository.existsByStatusAndRequesterShiftIdOrStatusAndTargetShiftId(
                SwapRequestStatus.PENDING, requesterShift.getId(), SwapRequestStatus.PENDING, targetShift.getId())) {
            throw new ConflictingSwapRequestException(requesterShift.getId());
        }

        SwapRequest swapRequest = new SwapRequest(
                requester, requesterShift, targetEmployee, targetShift, command.reason());

        return SwapRequestView.from(swapRequestRepository.save(swapRequest));
    }

    @Override
    @Transactional
    public SwapRequestView approve(SwapRequestDecisionCommand command) {
        return decide(command, true);
    }

    @Override
    @Transactional
    public SwapRequestView reject(SwapRequestDecisionCommand command) {
        return decide(command, false);
    }

    private SwapRequestView decide(SwapRequestDecisionCommand command, boolean approve) {
        SwapRequest swapRequest = getSwapRequestOrThrow(command.swapRequestId());
        Employee decider = getEmployeeOrThrow(command.decidingEmployeeId());
        if (approve) {
            swapRequest.approve(decider, command.note());
        } else {
            swapRequest.reject(decider, command.note());
        }
        return SwapRequestView.from(swapRequestRepository.save(swapRequest));
    }

    @Override
    @Transactional(readOnly = true)
    public SwapRequestView getById(Long swapRequestId) {
        return SwapRequestView.from(getSwapRequestOrThrow(swapRequestId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SwapRequestView> list(SwapRequestStatus statusFilter, Long employeeIdFilter) {
        List<SwapRequest> results;
        if (statusFilter != null) {
            results = swapRequestRepository.findByStatus(statusFilter);
        } else if (employeeIdFilter != null) {
            results = swapRequestRepository.findByRequesterIdOrTargetEmployeeId(employeeIdFilter, employeeIdFilter);
        } else {
            results = swapRequestRepository.findAll();
        }

        return results.stream()
                .filter(sr -> employeeIdFilter == null
                        || sr.getRequester().getId().equals(employeeIdFilter)
                        || sr.getTargetEmployee().getId().equals(employeeIdFilter))
                .map(SwapRequestView::from)
                .toList();
    }

    private Employee getEmployeeOrThrow(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    private Shift getShiftOrThrow(Long id) {
        return shiftRepository.findById(id)
                .orElseThrow(() -> new ShiftNotFoundException(id));
    }

    private SwapRequest getSwapRequestOrThrow(Long id) {
        return swapRequestRepository.findById(id)
                .orElseThrow(() -> new SwapRequestNotFoundException(id));
    }
}
