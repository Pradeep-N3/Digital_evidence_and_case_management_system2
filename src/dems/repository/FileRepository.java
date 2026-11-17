package dems.repository;

import dems.model.Identifiable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * GENERIC CLASS: a small file-based database. Every record is one line of a text file.
 * All records are also kept in a LinkedHashMap (a COLLECTION) for fast lookup.
 * How a record becomes a line (and back) is supplied as two LAMBDAS.
 */
public class FileRepository<T extends Identifiable<String>> implements Repository<T, String> {

    private final Path file;
    private final Function<T, String> encoder;
    private final Function<String, T> decoder;
    private final Map<String, T> store = new LinkedHashMap<>();

    public FileRepository(Path file, Function<T, String> encoder, Function<String, T> decoder) throws IOException {
        this.file = file;
        this.encoder = encoder;
        this.decoder = decoder;
        load();
    }

    private void load() throws IOException {
        if (!Files.exists(file)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                try {
                    T item = decoder.apply(line);
                    store.put(item.getId(), item);
                } catch (RuntimeException e) {
                    System.err.println("Skipping unreadable record in " + file.getFileName() + ": " + e);
                }
            }
        }
    }

    /** Writes to a temporary file first, then swaps it in, so a crash cannot corrupt the database. */
    private void persist() throws IOException {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            for (T item : store.values()) {
                writer.write(encoder.apply(item));
                writer.newLine();
            }
        }
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public synchronized void save(T item) throws IOException {
        store.put(item.getId(), item);
        persist();
    }

    @Override
    public synchronized void saveAll(Collection<T> items) throws IOException {
        for (T item : items) {
            store.put(item.getId(), item);
        }
        persist();
    }

    @Override
    public synchronized Optional<T> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public synchronized List<T> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public synchronized List<T> findWhere(Predicate<T> filter) {
        return store.values().stream().filter(filter).collect(Collectors.toList());
    }

    @Override
    public synchronized boolean delete(String id) throws IOException {
        boolean removed = store.remove(id) != null;
        if (removed) {
            persist();
        }
        return removed;
    }

    @Override
    public synchronized boolean exists(String id) {
        return store.containsKey(id);
    }
}
