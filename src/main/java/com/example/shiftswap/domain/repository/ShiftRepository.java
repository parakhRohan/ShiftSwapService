package com.example.shiftswap.domain.repository;

import com.example.shiftswap.domain.model.Shift;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftRepository extends JpaRepository<Shift, Long> {
}
