# Assignment 4 Facade Report

**Student:** Assem Tugelbay  
**Group:** SE-2540  
**Topic:** 19 — Telemedicine

## Goal

The program books an online medical consultation through one simple facade. The three assigned subsystems do the work:

1. Appointment Scheduler and Patient Record hold the patient's appointment slot.
2. Video Call Manager starts the video call and stores Session Notes.
3. E-Prescription Service and Billing finalize the consultation.

The state of each subsystem and the workflow progress are stored in CSV files. A new facade instance can therefore continue a visit after the program is restarted.

## Design

`BankingFacade` is the common workflow facade. In this project its behavior is telemedicine-specific: it validates a consultation request, runs Appointment → Video Call → E-Prescription/Billing, saves a chain record after each committed step and compensates completed steps after a downstream failure.

`OperationsFacade` is an administrative facade. It combines the persisted state into one inspection view, exposes manual compensation and resets the demo files.

Each subsystem has an entity with legal status transitions, a service and its own persistence gateway. The subsystems do not import or call one another. Only the facade knows the complete workflow.

## Compensation scenarios

For a B failure, the patient appointment is already held and the chain is `PENDING_B`. The facade releases the appointment and records `COMPENSATED_A`.

For a C failure, the appointment is held and the video session notes are saved. The chain is `PENDING_C`. The facade cancels the video session first, releases the appointment second and records `COMPENSATED_B_A`.

## Realistic validation failure

The facade rejects a request without a patient, clinician, slot or session notes before creating a chain record.

## Testing

JUnit tests use a temporary directory for every scenario. They cover successful booking, both compensation depths, reload of a persisted `PENDING_B` visit, invalid input, direct use of all three subsystems and the administrative facade. The helper `givenVisit` keeps setup short and readable. JaCoCo is configured in the `coverage` Maven profile with an 80% line coverage check.

## Facade and Adapter

An Adapter changes an existing interface into another interface expected by a client. This problem is different: the client needs one higher-level consultation operation that coordinates three services, persists progress and decides how to undo partial work. Therefore the Facade pattern matches the design.

## Live demonstration

Run `mvn -q compile exec:java`, then choose:

- `1` to book a successful consultation;
- `2` to fail the video call manager and release A;
- `3` to fail e-prescription/billing and cancel B before releasing A;
- `4` to inspect CSV-backed patient, video-session, prescription/billing and chain state;
- `5`, restart the program, then `6` to demonstrate persistence reload;
- `8` to reset the demo.

The project contains the complete source, tests, UML diagram and this report.
