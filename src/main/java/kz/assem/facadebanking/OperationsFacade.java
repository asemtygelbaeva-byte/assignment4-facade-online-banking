package kz.assem.facadebanking;

public final class OperationsFacade {
    private final BankingFacade banking;
    private final AccountLedger ledger;
    private final ComplianceChecker compliance;
    private final SettlementConfirmer settlement;

    public OperationsFacade(BankingFacade banking, AccountLedger ledger,
                            ComplianceChecker compliance, SettlementConfirmer settlement) {
        this.banking = banking;
        this.ledger = ledger;
        this.compliance = compliance;
        this.settlement = settlement;
    }

    public String inspect() {
        StringBuilder output = new StringBuilder();
        output.append("ACCOUNTS\n");
        ledger.accounts().forEach(account -> output.append("  ").append(account).append('\n'));
        output.append("COMPLIANCE\n");
        compliance.cases().forEach(item -> output.append("  ").append(item).append('\n'));
        output.append("SETTLEMENTS\n");
        settlement.settlements().forEach(item -> output.append("  ").append(item).append('\n'));
        output.append("CHAIN\n");
        banking.chains().forEach(item -> output.append("  ").append(item.id()).append(" stage=")
                .append(item.stage()).append(" message=").append(item.message()).append('\n'));
        return output.toString();
    }

    public void manuallyCompensate(String transferId) {
        banking.compensatePersisted(transferId);
    }

    public void reset() {
        ledger.clear();
        compliance.clear();
        settlement.clear();
        banking.clearChains();
    }
}
