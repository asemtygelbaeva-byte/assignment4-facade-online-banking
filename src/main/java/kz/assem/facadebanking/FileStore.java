package kz.assem.facadebanking;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class FileStore {
    private final Path directory;

    public FileStore(Path directory) {
        this.directory = directory;
    }

    public List<String> read(String fileName) {
        Path file = directory.resolve(fileName);
        try {
            return Files.exists(file) ? Files.readAllLines(file) : List.of();
        } catch (IOException exception) {
            throw new BankingException("Cannot read " + fileName + ": " + exception.getMessage());
        }
    }

    public void write(String fileName, List<String> lines) {
        try {
            Files.createDirectories(directory);
            Files.write(directory.resolve(fileName), lines);
        } catch (IOException exception) {
            throw new BankingException("Cannot write " + fileName + ": " + exception.getMessage());
        }
    }

    public void clear(String fileName) {
        write(fileName, List.of());
    }
}
