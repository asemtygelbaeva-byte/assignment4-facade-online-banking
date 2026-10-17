package kz.assem.facadebanking;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ComplianceStore {
    private static final String FILE = "compliance.csv";
    private final FileStore files;

    public ComplianceStore(Path directory) {
        this.files = new FileStore(directory);
    }

    public List<ComplianceCase> all() {
        List<ComplianceCase> cases = new ArrayList<>();
        for (String line : files.read(FILE)) {
            String[] parts = line.split("\\|", -1);
            cases.add(new ComplianceCase(parts[0], ComplianceCase.Status.valueOf(parts[1]), parts[2]));
        }
        return cases;
    }

    public ComplianceCase find(String id) {
        return all().stream().filter(item -> item.transferId().equals(id)).findFirst()
                .orElseThrow(() -> new BankingException("Unknown compliance case: " + id));
    }

    public void saveAll(List<ComplianceCase> cases) {
        files.write(FILE, cases.stream().map(item -> String.join("|", item.transferId(),
                item.status().name(), item.reason())).toList());
    }

    public void clear() { files.clear(FILE); }
}
