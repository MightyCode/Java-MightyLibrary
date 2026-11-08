package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.main.MainThread;
import io.github.mightycode.mightylib.main.Workers;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The queues of the loading pipeline: what other threads post to the main thread, the data to update, the steps run
 * on the main thread and the steps run by the workers (at most maxNumberOfWorkers at once).
 * Everything is main thread only, except post and runOnMainThread (any thread).
 */
class ResourceScheduler {
    private static class WorkerStep {
        final DataType data;
        final Runnable run;

        WorkerStep(DataType data, Runnable run) {
            this.data = data;
            this.run = run;
        }
    }

    private final Resources resources;
    private final int maxNumberOfWorkers;

    // Any thread -> main thread: calls from workers and ends of worker steps
    private final ConcurrentLinkedQueue<Runnable> inbox = new ConcurrentLinkedQueue<>();

    // Main thread only
    private final ArrayDeque<DataType> toUpdate = new ArrayDeque<>();
    private final ArrayDeque<Runnable> mainContextSteps = new ArrayDeque<>();
    private final ArrayDeque<WorkerStep> workerSteps = new ArrayDeque<>();
    private int runningWorkerSteps;
    // Queued steps dropped before starting because their result was no longer needed (see stepStillNeeded)
    private int skippedSteps;

    ResourceScheduler(Resources resources, int maxNumberOfWorkers) {
        this.resources = resources;
        this.maxNumberOfWorkers = Math.max(1, maxNumberOfWorkers);
    }

    // Any thread: run now on the main thread, posted to it otherwise
    void runOnMainThread(Runnable task) {
        if (MainThread.isMainThread())
            task.run();
        else
            inbox.add(task);
    }

    void enqueueUpdate(DataType data) {
        toUpdate.add(data);
    }

    /**
     * Apply the results of the workers, update the data and run the main context steps
     * until the time is spent (at least one step), or until nothing is left with -1.
     */
    void process(int allowedMilliseconds) {
        long startTime = System.currentTimeMillis();
        boolean first = true;

        while (first || allowedMilliseconds == -1 || System.currentTimeMillis() - startTime < allowedMilliseconds) {
            first = false;

            Runnable fromInbox;
            while ((fromInbox = inbox.poll()) != null)
                fromInbox.run();

            DataType data;
            while ((data = toUpdate.poll()) != null)
                resources.update(data);

            startWorkerSteps();

            Runnable step = mainContextSteps.poll();
            if (step == null)
                return;

            step.run();
        }
    }

    // True when nothing is queued or running
    boolean isIdle() {
        return inbox.isEmpty() && toUpdate.isEmpty() && mainContextSteps.isEmpty() && workerSteps.isEmpty()
                && runningWorkerSteps == 0;
    }

    void describe(JSONObject pipeline) {
        pipeline.put("runningWorkerSteps", runningWorkerSteps);
        pipeline.put("waitingWorkerSteps", workerSteps.size());
        pipeline.put("mainContextSteps", mainContextSteps.size());
        pipeline.put("skippedSteps", skippedSteps);
    }

    // Run the step of the data (work then end) where its type wants it
    void submit(DataType data, Runnable work, Runnable end) {
        switch (data.getTypeSetUp()) {
            case IMMEDIATELY_IN_CURRENT_CONTEXT:
                work.run();
                end.run();
                break;
            case MAIN_CONTEXT:
                mainContextSteps.add(() -> {
                    if (!stepStillNeeded(data)) {
                        skipStep(data);
                        return;
                    }

                    work.run();
                    end.run();
                });
                break;
            case THREAD_CONTEXT:
                workerSteps.add(new WorkerStep(data, () -> {
                    work.run();
                    inbox.add(() -> {
                        --runningWorkerSteps;
                        end.run();
                    });
                }));
                startWorkerSteps();
                break;
        }
    }

    /**
     * Checked when a queued step is about to start (not when it is queued: a data unloaded then wanted again
     * meanwhile keeps its step). False when its result would be thrown away: the data is no longer wanted
     * (scene left during its loading, reload copy abandoned...) or must be read again from the start.
     */
    private boolean stepStillNeeded(DataType data) {
        return resources.isWanted(data) && !data.reloadWanted;
    }

    // The step is not run: update discards the data, or starts it again from the start
    private void skipStep(DataType data) {
        data.jobRunning = false;
        ++skippedSteps;
        resources.requestUpdate(data);
    }

    // A running step stopped itself (DataType.checkCancelled): counted with the skipped ones
    void stepCancelled() {
        ++skippedSteps;
    }

    private void startWorkerSteps() {
        WorkerStep step;
        while (runningWorkerSteps < maxNumberOfWorkers && (step = workerSteps.poll()) != null) {
            if (!stepStillNeeded(step.data)) {
                skipStep(step.data);
                continue;
            }

            ++runningWorkerSteps;
            Workers.execute(step.run);
        }
    }
}
