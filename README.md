# Assignment 4 Facade Over Online Banking

Student: Assem Tugelbay  
Group: SE-2540  
Domain: Online Banking, individual variant 2

This project demonstrates the Facade pattern over three independent banking subsystems. The client starts one transfer operation, while the facade coordinates Account Ledger, Compliance Checker and Settlement Confirmer in the order A -> B -> C.

The project is intentionally small and uses only Java 17, Maven, JUnit 5 and JaCoCo. Each subsystem has its own entity, business logic class and persistence gateway. State is saved as plain CSV files in `data/`, so it remains after the program is closed and restarted.

## Run the application

```bash
mvn -q compile exec:java
```

The CLI menu can:

- complete a successful A -> B -> C transfer;
- force a B failure and show compensation of A;
- force a C failure and show compensation of B followed by A;
- inspect all persisted state;
- stop after A, restart the program, and resume from the saved `PENDING_B` state;
- manually compensate a pending chain;
- reset the CSV store before a fresh demo.

The default demo uses `alice` and `bob`. To use another data directory, pass it as the first argument:

```bash
mvn -q compile exec:java -Dexec.args="/tmp/online-banking-data"
```

## Test and coverage commands

```bash
mvn -q test
mvn -q verify -Pcoverage
```

The tests are independent and create a clean temporary directory for every scenario. The coverage profile generates `target/site/jacoco/index.html` and checks at least 80% line coverage for the tested application code.

## Subsystems and classes

| Part | Classes | Own persisted file |
|---|---|---|
| A Account Ledger | `Account`, `AccountLedger`, `AccountStore` | `accounts.csv` |
| B Compliance Checker | `ComplianceCase`, `ComplianceChecker`, `ComplianceStore` | `compliance.csv` |
| C Settlement Confirmer | `Settlement`, `SettlementConfirmer`, `SettlementStore` | `settlements.csv` |
| Chain progress | `ChainRecord`, `ChainStore` | `chain.csv` |

Every subsystem can be used directly. `BankingFacadeTest.directSubsystemsRemainUsable` calls the three subsystem services without a facade.

## Facades

`BankingFacade` is the common client facade. It validates input, reserves funds, runs compliance, confirms settlement, persists each stage and compensates the correct part of the chain when a later stage fails.

`OperationsFacade` is the narrower administrative facade. It provides one inspection view, manual compensation and a reset operation for the live demo.

## Compensation behavior

When B fails after A has committed, the persisted chain is `PENDING_B`. The facade releases the reservation in A and saves `COMPENSATED_A`.

When C fails after A and B have committed, the persisted chain is `PENDING_C`. The facade cancels the approved compliance case in B first, then releases the reservation in A, and saves `COMPENSATED_B_A`.

The failure switches in the CLI represent realistic downstream unavailability. An amount above 5000 is also rejected by the compliance business rule, and invalid input is rejected before any subsystem commits.

## Persistence reload

The `start` operation saves A's reservation and `PENDING_B` to disk. A new `BankingFacade` can then read that record after a restart and continue with B and C. This is covered by `reloadsPersistedPendingStateAndResumes`.

## Principle of Least Knowledge

Before the facade, a client would need to know every subsystem and the correct order:

```java
ledger.reserve("alice", amount);
compliance.approve(request);
settlement.confirm(request);
```

It would also need to know when to release the ledger or cancel compliance after a failure. That creates many friends and exposes the chain details.

After the facade, the client has one immediate friend:

```java
banking.execute(request, FailurePoint.NONE);
```

The facade owns sequencing, persistence and compensation. The subsystems do not import or call one another.

## Facade versus Adapter

An Adapter changes one existing interface into another interface that a client already expects. This assignment needs a Facade because the client needs one new, higher-level operation built from several independent operations. The banking facade coordinates order, durable progress and compensation; it does not merely rename a method or translate an interface.

## UML

The class diagram is available as the vector file [`docs/class-diagram.svg`](docs/class-diagram.svg).

## Test scenarios

- successful transfer reaches `COMPLETED` and changes both account balances;
- B failure compensates A only;
- C failure cancels B, then compensates A;
- persisted `PENDING_B` survives a new facade instance and resumes;
- invalid input is rejected before persistence;
- all three subsystems work directly;
- the administrative facade inspects and resets persisted state.

## Author

Assem Tugelbay, SE-2540
