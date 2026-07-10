package com.example.shiftswap.service;

import com.example.shiftswap.domain.model.SwapRequestStatus;
import com.example.shiftswap.service.model.CreateSwapRequestCommand;
import com.example.shiftswap.service.model.SwapRequestDecisionCommand;
import com.example.shiftswap.service.model.SwapRequestView;
import java.util.List;
public interface SwapRequestService {
    SwapRequestView createSwapRequest(CreateSwapRequestCommand command);
    SwapRequestView approve(SwapRequestDecisionCommand command);
    SwapRequestView reject(SwapRequestDecisionCommand command);
    SwapRequestView getById(Long swapRequestId);
    List<SwapRequestView> list(SwapRequestStatus statusFilter, Long employeeIdFilter);
}
