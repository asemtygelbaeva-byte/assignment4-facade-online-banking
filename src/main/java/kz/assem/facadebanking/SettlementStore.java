package kz.assem.facadebanking;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class SettlementStore {
    private static final String FILE = "settlements.csv";
    private final FileStore files;

    public SettlementStore(Path directory) {
        this.files = new FileStore(directory);
    }

    public List<Settlement> all() {
        List<Settlement> settlements = new ArrayList<>();
        for (String line : files.read(FILE)) {
            String[] parts = line.split("\\|", -1);
            settlements.add(new Settlement(parts[0], new BigDecimal(parts[1]), Settlement.Status.valueOf(parts[2])));
        }
        return settlements;
    }

    public Settlement find(String id) {
        return all().stream().filter(item -> item.transferId().equals(id)).findFirst()
                .orElseThrow(() -> new BankingException("Unknown settlement: " + id));
    }

    public void saveAll(List<Settlement> settlements) {
        files.write(FILE, settlements.stream().map(item -> String.join("|", item.transferId(),
                item.amount().toPlainString(), item.status().name())).toList());
    }

    public void clear() { files.clear(FILE); }
}
