package kz.assem.facadebanking;

import java.nio.file.Path;
import java.util.ArrayList;

public final class ComplianceChecker {
    private final ComplianceStore store;

    public ComplianceChecker(Path directory) {
        this.store = new ComplianceStore(directory);
    }

    public ComplianceCase approve(TransferRequest request) {
        ComplianceCase complianceCase = new ComplianceCase(request.id(), ComplianceCase.Status.NEW, "");
        complianceCase.connect();
        complianceCase.saveNotes(request.sessionNotes());
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
