package kz.assem.facadebanking;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ChainStore {
    private static final String FILE = "chain.csv";
    private final FileStore files;

    public ChainStore(Path directory) {
        this.files = new FileStore(directory);
    }

    public List<ChainRecord> all() {
        List<ChainRecord> records = new ArrayList<>();
        for (String line : files.read(FILE)) {
            String[] parts = line.split("\\|", -1);
            records.add(new ChainRecord(parts[0], parts[1], parts[2], new BigDecimal(parts[3]),
                    ChainRecord.Stage.valueOf(parts[4]), parts[5]));
        }
        return records;
    }

    public ChainRecord find(String id) {
        return all().stream().filter(record -> record.id().equals(id)).findFirst()
                .orElseThrow(() -> new BankingException("Unknown chain: " + id));
    }

    public void save(ChainRecord record) {
        ArrayList<ChainRecord> records = new ArrayList<>(all());
        records.removeIf(item -> item.id().equals(record.id()));
        records.add(record);
        files.write(FILE, records.stream().map(item -> String.join("|", item.id(), item.source(), item.target(),
                item.amount().toPlainString(), item.stage().name(), item.message())).toList());
    }

    public void clear() { files.clear(FILE); }
}
