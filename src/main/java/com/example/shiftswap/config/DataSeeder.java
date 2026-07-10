package com.example.shiftswap.config;

import com.example.shiftswap.domain.model.Employee;
import com.example.shiftswap.domain.model.EmployeeRole;
import com.example.shiftswap.domain.model.Shift;
import com.example.shiftswap.domain.repository.EmployeeRepository;
import com.example.shiftswap.domain.repository.ShiftRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.seed-data", name = "enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final EmployeeRepository employeeRepository;
    private final ShiftRepository shiftRepository;

    public DataSeeder(EmployeeRepository employeeRepository, ShiftRepository shiftRepository) {

        this.employeeRepository = employeeRepository;
        this.shiftRepository = shiftRepository;
    }

    @Override
    public void run(String... args) {

        Employee alice = employeeRepository.save(
                new Employee("Alice", EmployeeRole.EMPLOYEE));

        Employee bob = employeeRepository.save(
                new Employee("Bob", EmployeeRole.EMPLOYEE));

        Employee david = employeeRepository.save(
                new Employee("David", EmployeeRole.EMPLOYEE));

        Employee emma = employeeRepository.save(
                new Employee("Emma", EmployeeRole.EMPLOYEE));

        Employee frank = employeeRepository.save(
                new Employee("Frank", EmployeeRole.EMPLOYEE));

        Employee manager1 = employeeRepository.save(
                new Employee("Carol", EmployeeRole.MANAGER));

        Employee manager2 = employeeRepository.save(
                new Employee("Mike", EmployeeRole.MANAGER));

        Shift aliceShift = shiftRepository.save(new Shift(
                alice,
                LocalDate.now().plusDays(1),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)));

        Shift bobShift = shiftRepository.save(new Shift(
                bob,
                LocalDate.now().plusDays(1),
                LocalTime.of(13, 0),
                LocalTime.of(21, 0)));

        Shift davidShift = shiftRepository.save(new Shift(
                david,
                LocalDate.now().plusDays(1),
                LocalTime.of(8, 0),
                LocalTime.of(16, 0)));

        Shift emmaShift = shiftRepository.save(new Shift(
                emma,
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(18, 0)));

        Shift frankShift = shiftRepository.save(new Shift(
                frank,
                LocalDate.now().plusDays(1),
                LocalTime.of(14, 0),
                LocalTime.of(22, 0)));

        log.info("========== DATA SEEDING COMPLETE ==========");

    }
}
