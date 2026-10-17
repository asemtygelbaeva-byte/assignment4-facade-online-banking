package kz.assem.facadebanking;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;

public final class ComplianceChecker {
    private final ComplianceStore store;

    public ComplianceChecker(Path directory) {
        this.store = new ComplianceStore(directory);
    }

    public ComplianceCase approve(TransferRequest request) {
        if (request.amount().compareTo(new BigDecimal("5000")) > 0) {
            throw new SubsystemFailureException("Compliance rejected a transfer above 5000");
        }
        ComplianceCase complianceCase = new ComplianceCase(request.id(), ComplianceCase.Status.NEW, "waiting");
        complianceCase.approve();
        ArrayList<ComplianceCase> cases = new ArrayList<>(store.all());
        cases.add(complianceCase);
        store.saveAll(cases);
        return complianceCase;
    }

    public void cancel(String transferId) {
        ArrayList<ComplianceCase> cases = new ArrayList<>(store.all());
        ComplianceCase complianceCase = store.find(transferId);
        complianceCase.cancel();
        replace(cases, complianceCase);
        store.saveAll(cases);
    }

    public java.util.List<ComplianceCase> cases() { return store.all(); }
    public void clear() { store.clear(); }

    private void replace(ArrayList<ComplianceCase> cases, ComplianceCase updated) {
        for (int index = 0; index < cases.size(); index++) {
            if (cases.get(index).transferId().equals(updated.transferId())) {
                cases.set(index, updated);
                return;
            }
        }
    }
}
