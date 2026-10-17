package kz.assem.facadebanking;

import java.math.BigDecimal;
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
                    new Account("alice", new BigDecimal("1000.00"), BigDecimal.ZERO, Account.Status.ACTIVE),
                    new Account("bob", new BigDecimal("200.00"), BigDecimal.ZERO, Account.Status.ACTIVE)));
        }
    }

    public void reserve(String source, BigDecimal amount) {
        List<Account> accounts = new ArrayList<>(store.all());
        Account account = find(accounts, source);
        account.reserve(amount);
        store.saveAll(accounts);
    }

    public void release(String source, BigDecimal amount) {
        List<Account> accounts = new ArrayList<>(store.all());
        find(accounts, source).release(amount);
        store.saveAll(accounts);
    }

    public void complete(String source, String target, BigDecimal amount) {
        List<Account> accounts = new ArrayList<>(store.all());
        Account sourceAccount = find(accounts, source);
        Account targetAccount = find(accounts, target);
        sourceAccount.complete(amount);
        targetAccount.credit(amount);
        store.saveAll(accounts);
    }

    public List<Account> accounts() { return store.all(); }

    public void clear() { store.clear(); }

    private Account find(List<Account> accounts, String id) {
        return accounts.stream().filter(account -> account.id().equals(id)).findFirst()
                .orElseThrow(() -> new InvalidTransferException("Unknown account: " + id));
    }
}
