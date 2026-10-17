# Assignment 4 Facade Report

Student: Assem Tugelbay  
Group: SE-2540  
Domain: Online Banking

## Goal

The application models one online bank transfer as a three-stage operation. The client calls one facade method, while three independent subsystems perform the real work:

1. Account Ledger reserves money.
2. Compliance Checker approves the transfer.
3. Settlement Confirmer finalizes the operation.

The state of every subsystem and the progress of the chain are stored in CSV files. This makes a restart testable and keeps the demo state visible.

## Design

The `BankingFacade` is the common workflow facade. It validates the request, runs A -> B -> C, writes a chain record after every committed step and performs compensation when a later step fails.

The `OperationsFacade` is an administrative facade. It combines persisted data into one inspection output, exposes manual compensation and resets the demo store.

Each subsystem is independent and has three simple parts: an entity with legal status transitions, a business logic service and a persistence gateway. The subsystems never import one another. Only the facade knows the complete chain.

## Compensation scenarios

For a B failure, A has already reserved funds and the chain is stored as `PENDING_B`. The facade releases that reservation and records `COMPENSATED_A`.

For a C failure, A is reserved and B is approved. The chain is stored as `PENDING_C`. The facade cancels B first and releases A second, then records `COMPENSATED_B_A`. The order is visible in the code and in the persisted statuses.

## Exceptions

Invalid transfer data is rejected before a chain is created. Compliance rejects transfers above 5000. The CLI also has explicit B and C failure controls so the examiner can demonstrate both compensation depths live.

## Testing

The JUnit tests use temporary directories, so every test starts with clean persisted state. They cover the happy path, both compensation depths, a persistence reload, invalid input, direct subsystem usage and the administrative facade. The custom helper `givenTransfer` makes test setup read like a small domain language. JaCoCo is configured in the `coverage` Maven profile with an 80% line coverage check.

## Facade and Adapter

An Adapter translates an existing interface into another interface expected by a client. This problem is different: the client needs a single higher-level operation that coordinates three services, persists progress and decides how to compensate partial work. Therefore the Facade pattern fits the intent. The client knows `BankingFacade`; it does not know the order or rollback rules of the three subsystems.

## Live demonstration

Run `mvn -q compile exec:java`, then choose:

- `1` for a successful transfer;
- `2` to fail B and show A compensation;
- `3` to fail C and show B-then-A compensation;
- `4` to inspect CSV-backed state;
- `5`, restart the program, then `6` to demonstrate persistence reload;
- `8` to reset the store before a new demonstration.

The project is buildable with Maven and contains the complete source, tests and UML diagram.
