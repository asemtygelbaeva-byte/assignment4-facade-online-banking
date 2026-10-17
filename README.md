# Assignment 4: Facade over Telemedicine

Student: Assem Tugelbay  
Group: SE-2540  
Topic: 19 — Telemedicine

The application books one telemedicine consultation through three independent subsystems. The client calls one facade, while the facade coordinates the assigned workflow A → B → C:

1. Appointment Scheduler and Patient Record hold an appointment.
2. Video Call Manager starts the call and saves session notes.
3. E-Prescription Service sends the prescription and Billing finalizes the visit.

The project uses Java 17, Maven, JUnit 5 and CSV files in `data/`. Each subsystem has its own entity, service and persistence gateway. No subsystem imports another subsystem.

## Run

```bash
mvn -q compile exec:java
```

The CLI can book a consultation, force a failure in B or C, inspect persisted state, stop after A, resume after a restart, compensate a pending visit and reset the demo. The default patient is `patient-001`.

For another data directory:

```bash
mvn -q compile exec:java -Dexec.args="/tmp/telemedicine-data"
```

## Tests and coverage

```bash
mvn -q test
mvn -q verify -Pcoverage
```

The tests use temporary directories and cover the happy path, both compensation depths, persistence reload, invalid input, direct subsystem use and the administrative facade. The coverage profile generates `target/site/jacoco/index.html` and checks at least 80% line coverage.

## Subsystems

| Part | Classes | Own persisted file |
|---|---|---|
| A Appointment Scheduler / Patient Record | `Account`, `AccountLedger`, `AccountStore` | `accounts.csv` |
| B Video Call Manager / Session Notes | `ComplianceCase`, `ComplianceChecker`, `ComplianceStore` | `compliance.csv` |
| C E-Prescription Service / Billing | `Settlement`, `SettlementConfirmer`, `SettlementStore` | `settlements.csv` |
| Workflow progress | `ChainRecord`, `ChainStore` | `chain.csv` |

The existing class names are kept so the Maven project stays small; their fields and behavior describe the assigned telemedicine components.

## Facades and compensation

`BankingFacade` is the client facade for the telemedicine workflow. `OperationsFacade` is the administrative facade for inspection, manual compensation and reset.

If B fails after A, the appointment hold is released and the chain becomes `COMPENSATED_A`. If C fails after A and B, the video session is cancelled first, then the appointment hold is released; the chain becomes `COMPENSATED_B_A`.

Missing patient, clinician, slot or session notes is rejected before the chain starts. This is an additional realistic intake failure.

## Principle of Least Knowledge

Before the facade, a client had to know the order and rollback rules:

```java
appointmentScheduler.reserve(patient, clinician, slot);
videoCallManager.start(request);
ePrescriptionAndBilling.confirm(request);
```

After the facade, the client has one immediate friend:

```java
telemedicine.execute(request, FailurePoint.NONE);
```

The facade owns sequencing, persistence and compensation. The subsystems remain independent.

## Facade versus Adapter

An Adapter translates one existing interface into another interface expected by a client. This task needs a Facade because one new high-level consultation operation must coordinate three independent services, persist progress and compensate partial work.

## UML and report

The class diagram is [`docs/class-diagram.svg`](docs/class-diagram.svg). The full report is in [`REPORT.docx`](REPORT.docx) and [`REPORT.md`](REPORT.md).

## Author

Assem Tugelbay, SE-2540
