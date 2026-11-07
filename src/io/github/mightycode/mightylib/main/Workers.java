package io.github.mightycode.mightylib.main;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Fixed pool of worker threads owned by the engine, for short I/O and CPU tasks (no GL call: use MainThread).
 * Long or blocking loops (network, debug server, console) keep their own thread.
 */
public final class Workers {
    private static final int SHUTDOWN_WAIT_MILLISECONDS = 1000;

    private static ExecutorService executor;
    private static int threadCount;

    private Workers() {}

    // Called by MainLoop with EngineConfig.getMaxThreads, before anything uses the workers
    static synchronized void start(int count) {
        if (executor != null)
            return;

        threadCount = Math.max(1, count);
        AtomicInteger number = new AtomicInteger();
        ThreadFactory factory = task -> {
            Thread thread = new Thread(task, "MightyLibrary-Worker-" + number.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };

        executor = Executors.newFixedThreadPool(threadCount, factory);
    }

    // Without MainLoop (tools, tests), the pool is created with the default size at its first use
    private static synchronized ExecutorService get() {
        if (executor == null)
            start(Math.max(1, Runtime.getRuntime().availableProcessors() - 2));

        return executor;
    }

    public static synchronized int getThreadCount() {
        return executor == null ? 0 : threadCount;
    }

    public static Executor executor() {
        return Workers::execute;
    }

    public static void execute(Runnable task) {
        get().execute(task);
    }

    public static CompletableFuture<Void> run(Runnable task) {
        return CompletableFuture.runAsync(task, get());
    }

    public static <T> CompletableFuture<T> supply(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, get());
    }

    // Let the running tasks end, then stop the threads
    static synchronized void stop() {
        if (executor == null)
            return;

        executor.shutdown();
        try {
            if (!executor.awaitTermination(SHUTDOWN_WAIT_MILLISECONDS, TimeUnit.MILLISECONDS)) {
                System.err.println("--Workers still running after " + SHUTDOWN_WAIT_MILLISECONDS + " ms, interrupted");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        executor = null;
    }
}
