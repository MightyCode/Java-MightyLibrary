package io.github.mightycode.mightylib.resources;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Resource without file nor GL: counts its loads and unloads and records any inconsistency
 * (loaded twice, unloaded while not loaded, loaded before a dependency, loaded on a wrong thread).
 */
public class FakeData extends DataType {
    final AtomicInteger loads = new AtomicInteger();
    final AtomicInteger unloads = new AtomicInteger();
    final AtomicInteger preloads = new AtomicInteger();
    final AtomicInteger errors = new AtomicInteger();

    private volatile boolean live;

    // Read in the preload step, set by the test
    volatile int preloadMilliseconds;
    volatile boolean failLoad;
    volatile FakeData dependencyToAdd;

    private volatile Thread mainThread;

    public FakeData(TYPE_SET_UP typeSetUp, String name) {
        super(typeSetUp, name);
    }

    // Main context data must be loaded on this thread
    FakeData expectMainThread(Thread thread) {
        mainThread = thread;
        return this;
    }

    void preloadStep() {
        preloads.incrementAndGet();
        if (preloadMilliseconds > 0) {
            try {
                Thread.sleep(preloadMilliseconds);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        FakeData dependency = dependencyToAdd;
        if (dependency != null)
            addDependency(dependency);
    }

    @Override
    protected boolean internLoad() {
        if (live)
            error("loaded twice");

        if (!canBeLoad())
            error("loaded before its dependencies");

        if (mainThread != null && Thread.currentThread() != mainThread)
            error("loaded on " + Thread.currentThread().getName());

        if (failLoad)
            return false;

        live = true;
        loads.incrementAndGet();
        return true;
    }

    @Override
    protected void internUnload() {
        if (!live)
            error("unloaded while not loaded");

        live = false;
        unloads.incrementAndGet();
    }

    private void error(String message) {
        errors.incrementAndGet();
        System.err.println("--FakeData " + getDataName() + ": " + message);
    }

    int referenceCount() {
        return references.size();
    }
}
