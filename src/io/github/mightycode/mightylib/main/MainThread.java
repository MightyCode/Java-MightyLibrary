package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.utils.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * The main thread owns the GL context. Other threads come back to it only through this queue,
 * emptied by MainLoop at each frame within the time left in the frame.
 */
public final class MainThread {
    private static volatile Thread mainThread;
    private static final ConcurrentLinkedQueue<Runnable> TASKS = new ConcurrentLinkedQueue<>();

    // To continue a CompletableFuture on the main thread: future.thenAcceptAsync(action, MainThread.EXECUTOR)
    public static final Executor EXECUTOR = MainThread::execute;

    private MainThread() {}

    // Called once by MainLoop, from the thread that creates the GL context
    static void setCurrentAsMain() {
        mainThread = Thread.currentThread();
    }

    // True before the engine starts too: there is no other thread to protect yet
    public static boolean isMainThread() {
        Thread main = mainThread;
        return main == null || main == Thread.currentThread();
    }

    // Run the task at a next frame, on the main thread (also when called from it).
    // The task runs with the view that was current when it was posted (none from a worker thread).
    public static void execute(Runnable task) {
        IView view = Views.currentOrNull();
        if (view == null) {
            TASKS.add(task);
            return;
        }

        TASKS.add(() -> {
            try (Views.Scope ignored = Views.enter(view)) {
                task.run();
            }
        });
    }

    public static CompletableFuture<Void> run(Runnable task) {
        return supply(() -> {
            task.run();
            return null;
        });
    }

    public static <T> CompletableFuture<T> supply(Supplier<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        execute(() -> {
            try {
                future.complete(task.get());
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    public static int pendingTasks() {
        return TASKS.size();
    }

    /**
     * Run the waiting tasks until the deadline (System.nanoTime), at least one.
     * Tasks added while processing wait for the next call, so a task that queues itself again can't loop forever.
     */
    static void process(long deadlineNanos) {
        int count = TASKS.size();
        for (int i = 0; i < count; ++i) {
            if (i > 0 && System.nanoTime() >= deadlineNanos)
                return;

            Runnable task = TASKS.poll();
            if (task == null)
                return;

            try {
                task.run();
            } catch (Throwable e) {
                Logger.LogException("Main thread task failed", e);
            }
        }
    }

    // Run everything still waiting, at the exit
    static void processAll() {
        process(Long.MAX_VALUE);
    }
}
