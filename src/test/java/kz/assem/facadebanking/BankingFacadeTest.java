package kz.assem.facadebanking;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankingFacadeTest {
    @Test
    void completesTransferAndPersistsTheResult() throws Exception {
        World world = newWorld();

        world.banking.execute(givenTransfer("tx-success", "100.00"), FailurePoint.NONE);

        assertEquals(ChainRecord.Stage.COMPLETED, world.banking.chains().get(0).stage());
        assertEquals(new BigDecimal("900.00"), account(world, "alice").balance());
        assertEquals(new BigDecimal("300.00"), account(world, "bob").balance());
    }

    @Test
    void compensatesAWhenBFails() throws Exception {
        World world = newWorld();

        BankingException failure = assertThrows(BankingException.class,
                () -> world.banking.execute(givenTransfer("tx-b-fail", "100.00"), FailurePoint.B));

        assertTrue(failure.getMessage().contains("B is unavailable"));
        assertEquals(ChainRecord.Stage.COMPENSATED_A, world.banking.chains().get(0).stage());
        assertEquals(BigDecimal.ZERO.setScale(2), account(world, "alice").reserved());
        assertTrue(world.compliance.cases().isEmpty());
    }

    @Test
    void compensatesBThenAWhenCFails() throws Exception {
        World world = newWorld();

        assertThrows(BankingException.class,
                () -> world.banking.execute(givenTransfer("tx-c-fail", "100.00"), FailurePoint.C));

        assertEquals(ChainRecord.Stage.COMPENSATED_B_A, world.banking.chains().get(0).stage());
        assertEquals(ComplianceCase.Status.CANCELLED, world.compliance.cases().get(0).status());
        assertEquals(BigDecimal.ZERO.setScale(2), account(world, "alice").reserved());
        assertTrue(world.settlement.settlements().isEmpty());
    }

    @Test
    void reloadsPersistedPendingStateAndResumes() throws Exception {
        Path directory = Files.createTempDirectory("facade-reload");
        World firstRun = newWorld(directory);
        TransferRequest request = givenTransfer("tx-reload", "75.00");

        firstRun.banking.start(request);
        World afterRestart = newWorld(directory);
        afterRestart.banking.resume(request.id(), FailurePoint.NONE);

        assertEquals(ChainRecord.Stage.COMPLETED, afterRestart.banking.chains().get(0).stage());
        assertEquals(new BigDecimal("925.00"), account(afterRestart, "alice").balance());
    }

    @Test
    void rejectsInvalidInputBeforeAnythingIsCommitted() throws Exception {
        World world = newWorld();

        assertThrows(InvalidTransferException.class,
                () -> world.banking.execute(new TransferRequest("tx-invalid", "alice", "bob", BigDecimal.ZERO), FailurePoint.NONE));

        assertTrue(world.banking.chains().isEmpty());
        assertEquals(BigDecimal.ZERO, account(world, "alice").reserved());
    }

    @Test
    void directSubsystemsRemainUsable() throws Exception {
        World world = newWorld();

        world.ledger.reserve("alice", new BigDecimal("10.00"));
        world.compliance.approve(givenTransfer("tx-direct", "10.00"));
        world.settlement.confirm(givenTransfer("tx-direct", "10.00"));

        assertEquals(Account.Status.RESERVED, account(world, "alice").status());
        assertEquals(ComplianceCase.Status.APPROVED, world.compliance.cases().get(0).status());
        assertEquals(Settlement.Status.CONFIRMED, world.settlement.settlements().get(0).status());
    }

    @Test
    void operationsFacadeCanInspectAndReset() throws Exception {
        World world = newWorld();
        world.banking.execute(givenTransfer("tx-admin", "20.00"), FailurePoint.NONE);

        assertTrue(world.operations.inspect().contains("tx-admin"));
        world.operations.reset();

        assertTrue(world.operations.inspect().contains("ACCOUNTS"));
        assertTrue(world.banking.chains().isEmpty());
    }

    private TransferRequest givenTransfer(String id, String amount) {
        return new TransferRequest(id, "alice", "bob", new BigDecimal(amount));
    }

    private Account account(World world, String id) {
        return world.ledger.accounts().stream().filter(item -> item.id().equals(id)).findFirst().orElseThrow();
    }

    private World newWorld() throws Exception {
        return newWorld(Files.createTempDirectory("facade-test"));
    }

    private World newWorld(Path directory) {
        AccountLedger ledger = new AccountLedger(directory);
        ComplianceChecker compliance = new ComplianceChecker(directory);
        SettlementConfirmer settlement = new SettlementConfirmer(directory);
        ChainStore chains = new ChainStore(directory);
        ledger.seedDefaults();
        BankingFacade banking = new BankingFacade(ledger, compliance, settlement, chains);
        return new World(ledger, compliance, settlement, banking,
                new OperationsFacade(banking, ledger, compliance, settlement));
    }

    private record World(AccountLedger ledger, ComplianceChecker compliance,
                         SettlementConfirmer settlement, BankingFacade banking,
                         OperationsFacade operations) { }
}
