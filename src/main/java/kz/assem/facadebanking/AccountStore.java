package kz.assem.facadebanking;

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
            accounts.add(new Account(parts[0], parts[1], parts[2], parts[3], Account.Status.valueOf(parts[4])));
        }
        return accounts;
    }

    public Account find(String id) {
        return all().stream().filter(account -> account.id().equals(id)).findFirst()
                .orElseThrow(() -> new InvalidTransferException("Unknown patient: " + id));
    }

    public void saveAll(List<Account> accounts) {
        files.write(FILE, accounts.stream().map(account -> String.join("|", account.id(), account.patientName(),
                account.clinician(), account.slot(), account.status().name())).toList());
    }

    public void clear() {
        files.clear(FILE);
    }
}
