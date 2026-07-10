package com.example.shiftswap.domain.repository;

import com.example.shiftswap.domain.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
}
