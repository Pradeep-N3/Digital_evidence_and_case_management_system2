package dems.service;

import dems.model.Evidence;
import dems.model.IntegrityResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * MULTITHREADING (thread pool): re-hashes many evidence files in parallel.
 * Each file becomes one Callable task; a fixed pool of worker threads runs them concurrently.
 */
public class IntegrityScanner {

    private final int maxThreads;

    public IntegrityScanner(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public Map<String, IntegrityResult> scan(Collection<Evidence> items) throws InterruptedException {
        Map<String, IntegrityResult> results = new TreeMap<>();
        if (items.isEmpty()) {
            return results;
        }
        List<Evidence> list = new ArrayList<>(items);
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(maxThreads, list.size()));
        try {
            List<Callable<IntegrityResult>> tasks = new ArrayList<>();
            for (Evidence e : list) {
                tasks.add(e::verifyIntegrity); // method reference used as a Callable
            }
            List<Future<IntegrityResult>> futures = pool.invokeAll(tasks);
            for (int i = 0; i < list.size(); i++) {
                try {
                    results.put(list.get(i).getId(), futures.get(i).get());
                } catch (ExecutionException ex) {
                    results.put(list.get(i).getId(), IntegrityResult.MISSING);
                }
            }
        } finally {
            pool.shutdown();
        }
        return results;
    }
}
