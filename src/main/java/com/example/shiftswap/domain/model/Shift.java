package com.example.shiftswap.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

@Entity
@Table(name = "shifts")
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    protected Shift() {
    }

    public Shift(Employee employee, LocalDate shiftDate, LocalTime startTime, LocalTime endTime) {
        this.employee = Objects.requireNonNull(employee, "employee must not be null");
        this.shiftDate = Objects.requireNonNull(shiftDate, "shiftDate must not be null");
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
    }

    public Long getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public LocalDate getShiftDate() {
        return shiftDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    void reassignTo(Employee newOwner) {
        this.employee = Objects.requireNonNull(newOwner, "newOwner must not be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Shift other)) {
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
        return "Shift{id=%d, employeeId=%s, date=%s, %s-%s}"
                .formatted(id, employee != null ? employee.getId() : null, shiftDate, startTime, endTime);
    }
}
