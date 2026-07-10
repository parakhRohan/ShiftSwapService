# Shift Swap Request and Approval Service

A small Spring Boot service implementing the core workflow: an employee requests to swap their shift with another employee's shift, and a manager approves or rejects the request.

This README covers setup, running, testing, and the current API contract.

## Assumptions

Scope was kept deliberately small, per the exercise instructions. The following are explicit simplifications rather than oversights:

1. Authentication is simulated with an `X-Employee-Id` header. There is no login flow, JWT, or session.
2. Authorization is a single manager/employee role, not per-department or per-team scoping.
3. Employees can have multiple shifts.
4. Employees and shifts are pre-existing data. This service does not include endpoints to create employees or shifts.
5. No notification or email side-effects.
6. A shift can only be part of one pending swap request at a time.

## Prerequisites

- Java 21+
- Maven 3.9+ (or use the included Maven wrapper if available)

## Running locally

```bash
mvn spring-boot:run
```

This starts the service on `http://localhost:8080` backed by an in-memory H2 database.
On startup, a data seeder populates a small sample dataset for manual testing, including employees, a manager, and shifts you can use in the example calls below.

## API Contract

All create, approve, and reject calls use `X-Employee-Id` to identify the acting employee.

### Create a swap request

```http
POST /api/v1/swap-requests
X-Employee-Id: 1
Content-Type: application/json

{
  "requesterShiftId": 1,
  "targetEmployeeId": 2,
  "targetShiftId": 2,
  "reason": "family event"
}
```

### Approve a request

```http
POST /api/v1/swap-requests/{id}/approve
X-Employee-Id: 3
Content-Type: application/json

{
  "note": "coverage confirmed"
}
```

### Reject a request

```http
POST /api/v1/swap-requests/{id}/reject
X-Employee-Id: 3
Content-Type: application/json

{
  "note": "no coverage available"
}
```

The `note` field is optional. Clients may send an empty JSON body if they do not need to include a note.

### Fetch and list requests

```http
GET /api/v1/swap-requests/{id}
GET /api/v1/swap-requests?status=PENDING
GET /api/v1/swap-requests?employeeId=1
```

## Example requests

Create a swap request:

```bash
curl -i -X POST http://localhost:8080/api/v1/swap-requests \
  -H "Content-Type: application/json" \
  -H "X-Employee-Id: 1" \
  -d '{
        "requesterShiftId": 1,
        "targetEmployeeId": 2,
        "targetShiftId": 2,
        "reason": "family event"
      }'
```

Approve it:

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
- `SwapRequestTest` for the domain state machine and business invariants.
- `SwapRequestServiceImplTest` for the application layer with mocked repositories.
- `SwapRequestControllerIntegrationTest` for end-to-end HTTP coverage against H2, including 403, 404, and 409 cases.
