package kz.assem.facadebanking;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class AccountStore {
    private static final String FILE = "accounts.csv";
    private final FileStore files;

    public AccountStore(Path directory) {
        this.files = new FileStore(directory);
    }

    public List<Account> all() {
        List<Account> accounts = new ArrayList<>();
        for (String line : files.read(FILE)) {
            String[] parts = line.split("\\|", -1);
            accounts.add(new Account(parts[0], new BigDecimal(parts[1]), new BigDecimal(parts[2]),
                    Account.Status.valueOf(parts[3])));
        }
        return accounts;
    }

    public Account find(String id) {
        return all().stream().filter(account -> account.id().equals(id)).findFirst()
                .orElseThrow(() -> new InvalidTransferException("Unknown account: " + id));
    }

    public void saveAll(List<Account> accounts) {
        files.write(FILE, accounts.stream().map(account -> String.join("|", account.id(),
                account.balance().toPlainString(), account.reserved().toPlainString(), account.status().name())).toList());
    }

    public void clear() {
        files.clear(FILE);
    }
}
