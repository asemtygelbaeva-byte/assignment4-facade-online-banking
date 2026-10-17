package kz.assem.facadebanking;

import java.util.List;

public final class BankingFacade {
    private final AccountLedger ledger;
    private final ComplianceChecker compliance;
    private final SettlementConfirmer settlement;
    private final ChainStore chains;

    public BankingFacade(AccountLedger ledger, ComplianceChecker compliance,
                         SettlementConfirmer settlement, ChainStore chains) {
        this.ledger = ledger;
        this.compliance = compliance;
        this.settlement = settlement;
        this.chains = chains;
    }

    public void execute(TransferRequest request, FailurePoint failurePoint) {
        start(request);
        resume(request.id(), failurePoint);
    }

    public void start(TransferRequest request) {
        validate(request);
        if (chains.all().stream().anyMatch(record -> record.id().equals(request.id()))) {
            throw new InvalidTransferException("Visit id already exists: " + request.id());
        }
        ledger.reserve(request.patientId(), request.clinician(), request.slot());
        chains.save(new ChainRecord(request.id(), request.patientId(), request.clinician(), request.slot(),
                request.sessionNotes(), ChainRecord.Stage.PENDING_B, "A committed: appointment slot held"));
    }

    public void resume(String transferId, FailurePoint failurePoint) {
        ChainRecord record = chains.find(transferId);
        try {
            if (record.stage() == ChainRecord.Stage.PENDING_B) {
                if (failurePoint == FailurePoint.B) {
                    throw new SubsystemFailureException("B is unavailable: video call manager failed");
                }
                compliance.approve(toRequest(record));
                chains.save(new ChainRecord(record.id(), record.patientId(), record.clinician(), record.slot(),
                        record.sessionNotes(), ChainRecord.Stage.PENDING_C, "B committed: video call and notes saved"));
                record = chains.find(transferId);
            }
            if (record.stage() == ChainRecord.Stage.PENDING_C) {
                if (failurePoint == FailurePoint.C) {
                    throw new SubsystemFailureException("C is unavailable: e-prescription or billing failed");
                }
                settlement.confirm(toRequest(record));
                ledger.complete(record.patientId());
                chains.save(new ChainRecord(record.id(), record.patientId(), record.clinician(), record.slot(),
                        record.sessionNotes(), ChainRecord.Stage.COMPLETED, "A-B-C telemedicine visit completed"));
            }
        } catch (BankingException exception) {
            compensatePersisted(transferId);
            throw exception;
        }
    }

    public void compensatePersisted(String transferId) {
        ChainRecord record = chains.find(transferId);
        if (record.stage() == ChainRecord.Stage.PENDING_B) {
            ledger.release(record.patientId());
            chains.save(new ChainRecord(record.id(), record.patientId(), record.clinician(), record.slot(),
                    record.sessionNotes(), ChainRecord.Stage.COMPENSATED_A, "B failed: appointment released"));
        } else if (record.stage() == ChainRecord.Stage.PENDING_C) {
            compliance.cancel(record.id());
            ledger.release(record.patientId());
            chains.save(new ChainRecord(record.id(), record.patientId(), record.clinician(), record.slot(),
                    record.sessionNotes(), ChainRecord.Stage.COMPENSATED_B_A, "C failed: video session cancelled, then appointment released"));
        }
    }

    public List<ChainRecord> chains() { return chains.all(); }

    public void clearChains() { chains.clear(); }

    private TransferRequest toRequest(ChainRecord record) {
        return new TransferRequest(record.id(), record.patientId(), record.clinician(), record.slot(), record.sessionNotes());
    }

    private void validate(TransferRequest request) {
        if (request.id() == null || request.id().isBlank()) throw new InvalidTransferException("Visit id is required");
        if (request.patientId() == null || request.patientId().isBlank()
                || request.clinician() == null || request.clinician().isBlank()
                || request.slot() == null || request.slot().isBlank()) {
            throw new InvalidTransferException("Patient, clinician and appointment slot are required");
        }
        if (request.sessionNotes() == null || request.sessionNotes().isBlank()) {
            throw new InvalidTransferException("Session notes are required");
        }
    }
}
