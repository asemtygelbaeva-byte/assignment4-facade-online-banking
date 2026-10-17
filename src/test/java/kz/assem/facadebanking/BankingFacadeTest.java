package kz.assem.facadebanking;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankingFacadeTest {
    @Test
    void booksConsultationAndPersistsTheResult() throws Exception {
        World world = newWorld();
        world.banking.execute(givenVisit("visit-success", "INS-100"), FailurePoint.NONE);
        assertEquals(ChainRecord.Stage.COMPLETED, world.banking.chains().get(0).stage());
        assertEquals(Account.Status.COMPLETED, patient(world, "patient-001").status());
        assertEquals(Settlement.Status.BILLED, world.settlement.settlements().get(0).status());
    }

    @Test
    void releasesAppointmentWhenVideoCallFails() throws Exception {
        World world = newWorld();
        BankingException failure = assertThrows(BankingException.class,
                () -> world.banking.execute(givenVisit("visit-b-fail", "INS-100"), FailurePoint.B));
        assertTrue(failure.getMessage().contains("B is unavailable"));
        assertEquals(ChainRecord.Stage.COMPENSATED_A, world.banking.chains().get(0).stage());
        assertEquals(Account.Status.REGISTERED, patient(world, "patient-001").status());
        assertTrue(world.compliance.cases().isEmpty());
    }

    @Test
    void cancelsVideoSessionThenReleasesAppointmentWhenFinalizationFails() throws Exception {
        World world = newWorld();
        assertThrows(BankingException.class,
                () -> world.banking.execute(givenVisit("visit-c-fail", "INS-100"), FailurePoint.C));
        assertEquals(ChainRecord.Stage.COMPENSATED_B_A, world.banking.chains().get(0).stage());
        assertEquals(ComplianceCase.Status.CANCELLED, world.compliance.cases().get(0).status());
        assertEquals(Account.Status.REGISTERED, patient(world, "patient-001").status());
        assertTrue(world.settlement.settlements().isEmpty());
    }

    @Test
    void reloadsPersistedPendingVisitAndResumes() throws Exception {
        Path directory = Files.createTempDirectory("telemedicine-reload");
        World firstRun = newWorld(directory);
        TransferRequest request = givenVisit("visit-reload", "INS-200");
        firstRun.banking.start(request);
        World afterRestart = newWorld(directory);
        afterRestart.banking.resume(request.id(), FailurePoint.NONE);
        assertEquals(ChainRecord.Stage.COMPLETED, afterRestart.banking.chains().get(0).stage());
        assertEquals(Account.Status.COMPLETED, patient(afterRestart, "patient-001").status());
    }

    @Test
    void rejectsMissingSessionNotesBeforeAnythingIsCommitted() throws Exception {
        World world = newWorld();
        assertThrows(InvalidTransferException.class,
                () -> world.banking.execute(new TransferRequest("visit-invalid", "patient-001",
                        "Dr. Lee", "2026-11-01 10:00", ""), FailurePoint.NONE));
        assertTrue(world.banking.chains().isEmpty());
        assertEquals(Account.Status.REGISTERED, patient(world, "patient-001").status());
    }

    @Test
    void directSubsystemsRemainUsable() throws Exception {
        World world = newWorld();
        TransferRequest request = givenVisit("visit-direct", "INS-300");
        world.ledger.reserve(request.patientId(), request.clinician(), request.slot());
        world.compliance.approve(request);
        world.settlement.confirm(request);
        assertEquals(Account.Status.HELD, patient(world, "patient-001").status());
        assertEquals(ComplianceCase.Status.NOTES_SAVED, world.compliance.cases().get(0).status());
        assertEquals(Settlement.Status.BILLED, world.settlement.settlements().get(0).status());
    }

    @Test
    void operationsFacadeCanInspectAndReset() throws Exception {
        World world = newWorld();
        world.banking.execute(givenVisit("visit-admin", "INS-400"), FailurePoint.NONE);
        assertTrue(world.operations.inspect().contains("visit-admin"));
        world.operations.reset();
        assertTrue(world.operations.inspect().contains("PATIENT REGISTRY"));
        assertTrue(world.banking.chains().isEmpty());
    }

    private TransferRequest givenVisit(String id, String notes) {
        return new TransferRequest(id, "patient-001", "Dr. Lee", "2026-11-01 10:00", notes);
    }

    private Account patient(World world, String id) {
        return world.ledger.accounts().stream().filter(item -> item.id().equals(id)).findFirst().orElseThrow();
    }

    private World newWorld() throws Exception {
        return newWorld(Files.createTempDirectory("telemedicine-test"));
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
