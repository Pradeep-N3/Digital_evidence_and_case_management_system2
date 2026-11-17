package dems.service;

import dems.util.RecordCodec;
import dems.util.TimeUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * MULTITHREADING (producer / consumer): any thread can call log(), which just drops a
 * message into a BlockingQueue. One dedicated background thread ("audit-writer") takes
 * messages from the queue and appends them to the audit file. The UI never waits for disk I/O.
 */
public class AuditLogger {

    private static final class Message {
        final String line;
        final CountDownLatch latch;

        Message(String line, CountDownLatch latch) {
            this.line = line;
            this.latch = latch;
        }
    }

    private static final Message STOP = new Message(null, null);

    private final BlockingQueue<Message> queue = new LinkedBlockingQueue<>();
    private final Path file;
    private final Thread worker;

    public AuditLogger(Path file) {
        this.file = file;
        this.worker = new Thread(this::drainQueue, "audit-writer");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    /** Called from any thread; returns immediately. */
    public void log(String user, String action, String details) {
        String line = RecordCodec.encode(TimeUtil.nowLong(), user, action, details);
        queue.add(new Message(line, null));
    }

    private void drainQueue() {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            while (true) {
                Message m = queue.take();
                if (m == STOP) {
                    break;
                }
                if (m.line != null) {
                    writer.write(m.line);
                    writer.newLine();
                    writer.flush();
                }
                if (m.latch != null) {
                    m.latch.countDown();
                }
            }
        } catch (IOException e) {
            System.err.println("Audit writer failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Waits until everything logged so far has been written to disk. */
    public void flush() {
        CountDownLatch latch = new CountDownLatch(1);
        queue.add(new Message(null, latch));
        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Returns every entry as {time, user, action, details}, oldest first. */
    public List<String[]> readAll() throws IOException {
        flush();
        List<String[]> entries = new ArrayList<>();
        if (!Files.exists(file)) {
            return entries;
        }
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (!line.trim().isEmpty()) {
                entries.add(RecordCodec.decode(line));
            }
        }
        return entries;
    }

    public void shutdown() {
        queue.add(STOP);
        try {
            worker.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
