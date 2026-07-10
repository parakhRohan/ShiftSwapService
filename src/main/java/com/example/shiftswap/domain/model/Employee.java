package com.example.shiftswap.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmployeeRole role;

    protected Employee() {
    }

    public Employee(String name, EmployeeRole role) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.role = Objects.requireNonNull(role, "role must not be null");
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public EmployeeRole getRole() {
        return role;
    }

    public boolean isManager() {
        return role == EmployeeRole.MANAGER;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Employee other)) {
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
        return "Employee{id=%d, name='%s', role=%s}".formatted(id, name, role);
    }
}
