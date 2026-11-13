package io.github.mightycode.mightylib.main;

// Gives tests access to the package-private parts of Workers
public final class WorkersTestAccess {
    private WorkersTestAccess() {}

    // The default pool has (cores - 2) threads: tests that count on parallel tasks must not depend on the machine
    public static synchronized void ensureThreads(int count) {
        if (Workers.getThreadCount() >= count)
            return;

        Workers.stop();
        Workers.start(count);
    }
}
