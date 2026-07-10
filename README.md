# Shift Swap Request and Approval Service

A small Spring Boot service implementing the core workflow: an employee
requests to swap their shift with another employee's shift, and a manager
approves or rejects the request.

This README covers setup, running, and testing. See `DESIGN.md` for the
data model, API contract, and key trade-offs, and `DEPLOYMENT.md` for the
deployment plan, rollback strategy, and monitoring signals.

## Assumptions

Scope was kept deliberately small, per the exercise instructions. The
following are explicit, documented simplifications rather than oversights:

1. **Authentication is simulated with an `X-Employee-Id` header.** There is
   no login flow, JWT, or session. A real deployment would sit behind an
   API gateway or auth filter that validates a token and forwards a
   verified employee id (see DESIGN.md's "Authentication is out of scope"
   section for what that would look like).
2. **Authorization is a single manager/employee role**, not a
   per-department or per-team scoping. A manager can approve any pending
   request in the system. Real org-chart-aware routing (e.g. "only this
   employee's direct manager can approve") is a natural extension but out
   of scope here.
3. **Employees can have multiple shifts**
4. **Employees and shifts are pre-existing data.** This service does not
   include endpoints to create employees or shifts; a seeder
   populates sample data for manual testing (see below). Those would exist
   in a real HR/scheduling system that this service integrates with.
5. **No notification/email side-effects.** A production version would emit
   a domain event on creation/approval/rejection for a notification service
   to consume; this is called out as a future extension in DESIGN.md.
6. **Single swap per pair of shifts.** A shift can only be part of one
   *pending* swap request at a time (enforced with a 409 Conflict on
   creation) — see the failure-mode discussion in DESIGN.md.

## Prerequisites

- Java 21+
- Maven 3.9+ (or use the included Maven wrapper if you add one; plain
  `mvn` is assumed here)

## Running locally

```bash
mvn spring-boot:run
```

This starts the service on `http://localhost:8080` backed by an in-memory
H2 database, seeded with two employees, one manager, and one shift each
(see application logs at startup for the exact generated ids — H2 identity
columns start at 1 in a fresh instance, so on a first run they will
typically be `Alice=1`, `Bob=2`, `Manager=3`, `aliceShift=1`, `bobShift=2`,
but always check the log line rather than assuming). The H2 console is
available at `http://localhost:8080/h2-console` (JDBC URL
`jdbc:h2:mem:shiftswap`, user `sa`, blank password).

### Example requests

Create a swap request (Alice requesting to swap with Bob):

```bash
curl -i -X POST http://localhost:8080/api/v1/swap-requests \
  -H "Content-Type: application/json" \
  -H "X-Employee-Id: 1" \
  -d '{
        "requesterId": 1,
        "requesterShiftId": 1,
        "targetEmployeeId": 2,
        "targetShiftId": 2,
        "reason": "family event"
      }'
```

Approve it (as the manager, id 3):

```bash
curl -i -X POST http://localhost:8080/api/v1/swap-requests/1/approve \
  -H "Content-Type: application/json" \
  -H "X-Employee-Id: 3" \
  -d '{"note": "coverage confirmed"}'
```

Reject it instead:

```bash
curl -i -X POST http://localhost:8080/api/v1/swap-requests/1/reject \
  -H "Content-Type: application/json" \
  -H "X-Employee-Id: 3" \
  -d '{"note": "no coverage available"}'
```

Fetch or list requests:

```bash
curl http://localhost:8080/api/v1/swap-requests/1
curl "http://localhost:8080/api/v1/swap-requests?status=PENDING"
curl "http://localhost:8080/api/v1/swap-requests?employeeId=1"
```

## Running tests

```bash
mvn test
```

This runs:
- **Domain unit tests** (`SwapRequestTest`) — the approval/rejection state
  machine and business invariants, with no Spring context and no database.
- **Service unit tests** (`SwapRequestServiceImplTest`) — the application
  layer with mocked repositories (Mockito).
- **Integration tests** (`SwapRequestControllerIntegrationTest`) — full
  HTTP requests through `MockMvc` against a real (H2) database, covering
  the happy path plus 403/404/409 failure modes and idempotent approval
  retries.
