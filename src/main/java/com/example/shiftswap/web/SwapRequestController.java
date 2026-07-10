package com.example.shiftswap.web;

import com.example.shiftswap.domain.model.SwapRequestStatus;
import com.example.shiftswap.service.SwapRequestService;
import com.example.shiftswap.service.model.CreateSwapRequestCommand;
import com.example.shiftswap.service.model.CreateSwapRequestRequest;
import com.example.shiftswap.service.model.SwapRequestDecisionCommand;
import com.example.shiftswap.service.model.SwapRequestDecisionRequest;
import com.example.shiftswap.service.model.SwapRequestView;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/swap-requests")
public class SwapRequestController {

    private static final String EMPLOYEE_ID_HEADER = "X-Employee-Id";

    private final SwapRequestService swapRequestService;

    public SwapRequestController(SwapRequestService swapRequestService) {
        this.swapRequestService = swapRequestService;
    }

    @PostMapping
    public ResponseEntity<SwapRequestView> create(
            @RequestHeader(EMPLOYEE_ID_HEADER) Long actingEmployeeId,
            @Valid @RequestBody CreateSwapRequestRequest request) {

        CreateSwapRequestCommand command = new CreateSwapRequestCommand(
                actingEmployeeId,
                request.requesterShiftId(),
                request.targetEmployeeId(),
                request.targetShiftId(),
                request.reason());

        SwapRequestView created = swapRequestService.createSwapRequest(command);
        return ResponseEntity.created(URI.create("/api/v1/swap-requests/" + created.id())).body(created);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<SwapRequestView> approve(
            @PathVariable Long id,
            @RequestHeader(EMPLOYEE_ID_HEADER) Long decidingEmployeeId,
            @Valid @RequestBody(required = false) SwapRequestDecisionRequest request) {

        String note = request != null ? request.note() : null;
        SwapRequestDecisionCommand command = new SwapRequestDecisionCommand(id, decidingEmployeeId, note);
        SwapRequestView result = swapRequestService.approve(command);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<SwapRequestView> reject(
            @PathVariable Long id,
            @RequestHeader(EMPLOYEE_ID_HEADER) Long decidingEmployeeId,
            @Valid @RequestBody(required = false) SwapRequestDecisionRequest request) {

        String note = request != null ? request.note() : null;
        SwapRequestDecisionCommand command = new SwapRequestDecisionCommand(id, decidingEmployeeId, note);
        SwapRequestView result = swapRequestService.reject(command);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SwapRequestView> getById(@PathVariable Long id) {
        SwapRequestView view = swapRequestService.getById(id);
        return ResponseEntity.ok(view);
    }

    @GetMapping
    public ResponseEntity<List<SwapRequestView>> list(
            @RequestParam(required = false) SwapRequestStatus status,
            @RequestParam(required = false) Long employeeId) {

        return ResponseEntity.ok(swapRequestService.list(status, employeeId));
    }
}
