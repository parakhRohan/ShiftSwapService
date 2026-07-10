package com.example.shiftswap.web;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.shiftswap.domain.model.Employee;
import com.example.shiftswap.domain.model.EmployeeRole;
import com.example.shiftswap.domain.model.Shift;
import com.example.shiftswap.domain.repository.EmployeeRepository;
import com.example.shiftswap.domain.repository.ShiftRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SwapRequestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Employee alice;
    private Employee bob;
    private Employee manager;
    private Shift aliceShift;
    private Shift bobShift;

    @BeforeEach
    void setUp() {
        alice = employeeRepository.save(new Employee("Alice", EmployeeRole.EMPLOYEE));
        bob = employeeRepository.save(new Employee("Bob", EmployeeRole.EMPLOYEE));
        manager = employeeRepository.save(new Employee("Manager", EmployeeRole.MANAGER));
        aliceShift = shiftRepository.save(new Shift(
                alice, LocalDate.of(2026, 8, 1), LocalTime.of(9, 0), LocalTime.of(17, 0)));
        bobShift = shiftRepository.save(new Shift(
                bob, LocalDate.of(2026, 8, 1), LocalTime.of(13, 0), LocalTime.of(21, 0)));
    }

    @Test
    void fullHappyPath_createThenApprove_swapsShiftOwnership() throws Exception {
        String createBody = objectMapper.writeValueAsString(Map.of(
                "requesterShiftId", aliceShift.getId(),
                "targetEmployeeId", bob.getId(),
                "targetShiftId", bobShift.getId(),
                "reason", "family event"
        ));

        String response = mockMvc.perform(post("/api/v1/swap-requests")
                        .header("X-Employee-Id", alice.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andReturn().getResponse().getContentAsString();

        Long swapRequestId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(post("/api/v1/swap-requests/" + swapRequestId + "/approve")
                        .header("X-Employee-Id", manager.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"approved, coverage confirmed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")));

        assertShiftOwner(aliceShift.getId(), bob.getId());
        assertShiftOwner(bobShift.getId(), alice.getId());
    }

    @Test
    void createRequest_whenActingEmployeeIsNotTheRequester_returns403() throws Exception {
        String createBody = objectMapper.writeValueAsString(Map.of(
                "requesterShiftId", aliceShift.getId(),
                "targetEmployeeId", bob.getId(),
                "targetShiftId", bobShift.getId()
        ));

        mockMvc.perform(post("/api/v1/swap-requests")
                        .header("X-Employee-Id", bob.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void approve_byNonManager_returns403() throws Exception {
        Long swapRequestId = createPendingSwapRequest();

        mockMvc.perform(post("/api/v1/swap-requests/" + swapRequestId + "/approve")
                        .header("X-Employee-Id", bob.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void approve_onUnknownSwapRequest_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/swap-requests/999999/approve")
                        .header("X-Employee-Id", manager.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void secondPendingRequestOnSameShift_returns409() throws Exception {
        createPendingSwapRequest();

        String secondCreateBody = objectMapper.writeValueAsString(Map.of(
                "requesterShiftId", aliceShift.getId(),
                "targetEmployeeId", bob.getId(),
                "targetShiftId", bobShift.getId()
        ));

        mockMvc.perform(post("/api/v1/swap-requests")
                        .header("X-Employee-Id", alice.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondCreateBody))
                .andExpect(status().isConflict());
    }

    @Test
    void approvingTwice_isIdempotentAndDoesNotDoubleSwap() throws Exception {
        Long swapRequestId = createPendingSwapRequest();

        mockMvc.perform(post("/api/v1/swap-requests/" + swapRequestId + "/approve")
                        .header("X-Employee-Id", manager.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")));

        mockMvc.perform(post("/api/v1/swap-requests/" + swapRequestId + "/approve")
                        .header("X-Employee-Id", manager.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")));

        assertShiftOwner(aliceShift.getId(), bob.getId());
        assertShiftOwner(bobShift.getId(), alice.getId());
    }

    @Test
    void rejectingAnAlreadyApprovedRequest_returns409() throws Exception {
        Long swapRequestId = createPendingSwapRequest();

        mockMvc.perform(post("/api/v1/swap-requests/" + swapRequestId + "/approve")
                        .header("X-Employee-Id", manager.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/swap-requests/" + swapRequestId + "/reject")
                        .header("X-Employee-Id", manager.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    void getById_onUnknownRequest_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/swap-requests/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listFilteredByStatus_returnsOnlyMatchingRequests() throws Exception {
        createPendingSwapRequest();

        mockMvc.perform(get("/api/v1/swap-requests").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].status", is("PENDING")));

        mockMvc.perform(get("/api/v1/swap-requests").param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    private Long createPendingSwapRequest() throws Exception {
        String createBody = objectMapper.writeValueAsString(Map.of(
                "requesterShiftId", aliceShift.getId(),
                "targetEmployeeId", bob.getId(),
                "targetShiftId", bobShift.getId()
        ));

        String response = mockMvc.perform(post("/api/v1/swap-requests")
                        .header("X-Employee-Id", alice.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private void assertShiftOwner(Long shiftId, Long expectedOwnerId) {
        Shift shift = shiftRepository.findById(shiftId).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(shift.getEmployee().getId()).isEqualTo(expectedOwnerId);
    }
}
