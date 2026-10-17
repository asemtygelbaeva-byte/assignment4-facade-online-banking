package kz.assem.facadebanking;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class AccountLedger {
    private final AccountStore store;

    public AccountLedger(Path directory) {
        this.store = new AccountStore(directory);
    }

    public void seedDefaults() {
        if (store.all().isEmpty()) {
            store.saveAll(List.of(
                    new Account("patient-001", "Aigerim", "", "", Account.Status.REGISTERED),
                    new Account("patient-002", "Dias", "", "", Account.Status.REGISTERED)));
        }
    }

    public void reserve(String patientId, String clinician, String slot) {
        List<Account> accounts = new ArrayList<>(store.all());
        Account account = find(accounts, patientId);
        account.hold(clinician, slot);
        store.saveAll(accounts);
    }

    public void release(String patientId) {
        List<Account> accounts = new ArrayList<>(store.all());
        find(accounts, patientId).release();
        store.saveAll(accounts);
    }

    public void complete(String patientId) {
        List<Account> accounts = new ArrayList<>(store.all());
        find(accounts, patientId).complete();
        store.saveAll(accounts);
    }

    public List<Account> accounts() { return store.all(); }

    public void clear() { store.clear(); }

    private Account find(List<Account> accounts, String id) {
        return accounts.stream().filter(account -> account.id().equals(id)).findFirst()
                .orElseThrow(() -> new InvalidTransferException("Unknown patient: " + id));
    }
}
