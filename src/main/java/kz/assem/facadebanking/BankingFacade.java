package kz.assem.facadebanking;

import java.math.BigDecimal;
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
            throw new InvalidTransferException("Transfer id already exists: " + request.id());
        }
        ledger.reserve(request.sourceAccount(), request.amount());
        chains.save(new ChainRecord(request.id(), request.sourceAccount(), request.targetAccount(),
                request.amount(), ChainRecord.Stage.PENDING_B, "A committed: funds reserved"));
    }

    public void resume(String transferId, FailurePoint failurePoint) {
        ChainRecord record = chains.find(transferId);
        try {
            if (record.stage() == ChainRecord.Stage.PENDING_B) {
                if (failurePoint == FailurePoint.B) {
                    throw new SubsystemFailureException("B is unavailable: compliance check failed");
                }
                compliance.approve(toRequest(record));
                chains.save(new ChainRecord(record.id(), record.source(), record.target(), record.amount(),
                        ChainRecord.Stage.PENDING_C, "B committed: transfer approved"));
                record = chains.find(transferId);
            }
            if (record.stage() == ChainRecord.Stage.PENDING_C) {
                if (failurePoint == FailurePoint.C) {
                    throw new SubsystemFailureException("C is unavailable: settlement failed");
                }
                settlement.confirm(toRequest(record));
                ledger.complete(record.source(), record.target(), record.amount());
                chains.save(new ChainRecord(record.id(), record.source(), record.target(), record.amount(),
                        ChainRecord.Stage.COMPLETED, "A-B-C completed"));
            }
        } catch (BankingException exception) {
            compensatePersisted(transferId);
            throw exception;
        }
    }

    public void compensatePersisted(String transferId) {
        ChainRecord record = chains.find(transferId);
        if (record.stage() == ChainRecord.Stage.PENDING_B) {
            ledger.release(record.source(), record.amount());
            chains.save(new ChainRecord(record.id(), record.source(), record.target(), record.amount(),
                    ChainRecord.Stage.COMPENSATED_A, "B failed: A released"));
        } else if (record.stage() == ChainRecord.Stage.PENDING_C) {
            compliance.cancel(record.id());
            ledger.release(record.source(), record.amount());
            chains.save(new ChainRecord(record.id(), record.source(), record.target(), record.amount(),
                    ChainRecord.Stage.COMPENSATED_B_A, "C failed: B cancelled, then A released"));
        }
    }

    public List<ChainRecord> chains() { return chains.all(); }

    public void clearChains() { chains.clear(); }

    private TransferRequest toRequest(ChainRecord record) {
        return new TransferRequest(record.id(), record.source(), record.target(), record.amount());
    }

    private void validate(TransferRequest request) {
        if (request.id() == null || request.id().isBlank()) throw new InvalidTransferException("Transfer id is required");
        if (request.sourceAccount() == null || request.targetAccount() == null
                || request.sourceAccount().equals(request.targetAccount())) {
            throw new InvalidTransferException("Source and target accounts must be different");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferException("Amount must be positive");
        }
    }
}
