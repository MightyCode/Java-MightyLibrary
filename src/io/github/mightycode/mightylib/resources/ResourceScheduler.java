package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.main.MainThread;
import io.github.mightycode.mightylib.main.Workers;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The queues of the loading pipeline: what other threads post to the main thread, the data to update, the steps run
 * on the main thread and the steps run by the workers (at most maxNumberOfWorkers at once).
 * The steps of the data wanted now (urgent) go before those of the data only preloaded (see Resources.preloadBatch).
 * Everything is main thread only, except post and runOnMainThread (any thread).
 */
class ResourceScheduler {
    private static class Step {
        final DataType data;
        final Runnable run;

        Step(DataType data, Runnable run) {
            this.data = data;
            this.run = run;
        }
    }

    /**
     * Steps in two levels: the urgent ones in arrival order, then the preload ones in arrival order.
     * A preload step is promoted in O(1) when its data becomes urgent (a data has at most one step queued).
     */
    private static class StepQueue {
        private final ArrayDeque<Step> urgent = new ArrayDeque<>();
        private final Map<DataType, Step> background = new LinkedHashMap<>();

        void add(Step step, boolean isUrgent) {
            if (isUrgent)
                urgent.add(step);
            else
                background.put(step.data, step);
        }

        // The next step to run, the urgent ones first
        Step poll() {
            Step step = urgent.poll();
            if (step != null)
                return step;

            return pollBackground();
        }

        Step pollUrgent() {
            return urgent.poll();
        }

        Step pollBackground() {
            if (background.isEmpty())
                return null;

            DataType first = background.keySet().iterator().next();
            return background.remove(first);
        }

        void promote(DataType data) {
            Step step = background.remove(data);
            if (step != null)
                urgent.add(step);
        }

        boolean hasUrgent() {
            return !urgent.isEmpty();
        }

        boolean isEmpty() {
            return urgent.isEmpty() && background.isEmpty();
        }

        int size() {
            return urgent.size() + background.size();
        }

        int backgroundSize() {
            return background.size();
        }
    }

    private final Resources resources;
    private final int maxNumberOfWorkers;

    // Any thread -> main thread: calls from workers and ends of worker steps
    private final ConcurrentLinkedQueue<Runnable> inbox = new ConcurrentLinkedQueue<>();

    // Main thread only
    private final ArrayDeque<DataType> toUpdate = new ArrayDeque<>();
    private final StepQueue mainContextSteps = new StepQueue();
    private final StepQueue workerSteps = new StepQueue();
    // Data whose step runs on a worker
    private final Set<DataType> runningData = Collections.newSetFromMap(new IdentityHashMap<>());
    private int runningWorkerSteps;
    // Started as preload steps: they never take the last worker, an urgent step always finds one
    private int runningBackgroundSteps;
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

    // The data is wanted now: its queued step goes before the preload ones
    void promote(DataType data) {
        mainContextSteps.promote(data);
        workerSteps.promote(data);
    }

    /**
     * Apply the results of the workers, update the data and run the main context steps
     * until the time is spent (at least one step), or until nothing is left with -1.
     */
    void process(int allowedMilliseconds) {
        processUntil(allowedMilliseconds < 0 ? Long.MAX_VALUE : System.nanoTime() + allowedMilliseconds * 1_000_000L);
    }

    /**
     * Same, until the deadline (System.nanoTime), Long.MAX_VALUE for no limit. The inbox and the updates
     * stop at the deadline too (what is left waits for the next call), after at least one of each.
     */
    void processUntil(long deadlineNanos) {
        boolean first = true;

        while (first || System.nanoTime() < deadlineNanos) {
            first = false;

            Runnable fromInbox;
            while ((fromInbox = inbox.poll()) != null) {
                fromInbox.run();
                if (System.nanoTime() >= deadlineNanos)
                    break;
            }

            DataType data;
            while ((data = toUpdate.poll()) != null) {
                resources.update(data);
                if (System.nanoTime() >= deadlineNanos)
                    break;
            }

            startWorkerSteps();

            if (System.nanoTime() >= deadlineNanos)
                return;

            Step step = mainContextSteps.poll();
            if (step == null)
                return;

            step.run.run();
        }
    }

    /**
     * True when nothing wanted now is queued or running. What is only preloaded may go on: a loading screen must not
     * wait for the scene that is loaded in the background. What the inbox and the updates hold is short lived.
     */
    boolean isIdle() {
        if (!inbox.isEmpty() || !toUpdate.isEmpty() || mainContextSteps.hasUrgent() || workerSteps.hasUrgent())
            return false;

        for (DataType data : runningData) {
            if (resources.isUrgent(data))
                return false;
        }

        return true;
    }

    // True when nothing at all is queued or running, preloads included
    boolean isCompletelyIdle() {
        return inbox.isEmpty() && toUpdate.isEmpty() && mainContextSteps.isEmpty() && workerSteps.isEmpty()
                && runningWorkerSteps == 0;
    }

    void describe(JSONObject pipeline) {
        pipeline.put("runningWorkerSteps", runningWorkerSteps);
        pipeline.put("waitingWorkerSteps", workerSteps.size());
        pipeline.put("mainContextSteps", mainContextSteps.size());
        pipeline.put("preloadWorkerSteps", workerSteps.backgroundSize());
        pipeline.put("preloadMainContextSteps", mainContextSteps.backgroundSize());
        pipeline.put("skippedSteps", skippedSteps);
    }

    // Run the step of the data (work then end) where its type wants it
    void submit(DataType data, Runnable work, Runnable end) {
        boolean urgent = resources.isUrgent(data);

        switch (data.getTypeSetUp()) {
            case IMMEDIATELY_IN_CURRENT_CONTEXT:
                work.run();
                end.run();
                break;
            case MAIN_CONTEXT:
                mainContextSteps.add(new Step(data, () -> {
                    if (!stepStillNeeded(data)) {
                        skipStep(data);
                        return;
                    }

                    work.run();
                    end.run();
                }), urgent);
                break;
            case THREAD_CONTEXT:
                workerSteps.add(new Step(data, () -> {
                    work.run();
                    inbox.add(() -> {
                        --runningWorkerSteps;
                        runningData.remove(data);
                        end.run();
                    });
                }), urgent);
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
        // One worker stays free for the urgent steps (with a single worker, the preloads wait for it to be free)
        int backgroundLimit = Math.max(1, maxNumberOfWorkers - 1);

        while (runningWorkerSteps < maxNumberOfWorkers) {
            Step step = workerSteps.pollUrgent();
            boolean background = false;

            if (step == null) {
                if (runningBackgroundSteps >= backgroundLimit)
                    return;

                step = workerSteps.pollBackground();
                background = true;
            }

            if (step == null)
                return;

            if (!stepStillNeeded(step.data)) {
                skipStep(step.data);
                continue;
            }

            ++runningWorkerSteps;
            runningData.add(step.data);
            Runnable run = step.run;
            if (background) {
                ++runningBackgroundSteps;
                Workers.execute(() -> {
                    run.run();
                    inbox.add(() -> --runningBackgroundSteps);
                });
            } else {
                Workers.execute(run);
            }
        }
    }
}
