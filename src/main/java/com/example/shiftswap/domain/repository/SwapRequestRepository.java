package com.example.shiftswap.domain.repository;

import com.example.shiftswap.domain.model.SwapRequest;
import com.example.shiftswap.domain.model.SwapRequestStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SwapRequestRepository extends JpaRepository<SwapRequest, Long> {

    List<SwapRequest> findByStatus(SwapRequestStatus status);
    List<SwapRequest> findByRequesterIdOrTargetEmployeeId(Long requesterId, Long targetEmployeeId);
    boolean existsByStatusAndRequesterShiftIdOrStatusAndTargetShiftId(
            SwapRequestStatus status1, Long requesterShiftId, SwapRequestStatus status2, Long targetShiftId);
}
