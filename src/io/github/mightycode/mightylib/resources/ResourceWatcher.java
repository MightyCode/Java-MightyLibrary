package io.github.mightycode.mightylib.resources;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Hot reload on file change: when a source file of a selected, loaded data changes, the data is read again
 * (Resources.reload). Off until the code or a command asks for it (Resources.watcher(), command "watch",
 * system property mightylib.resources.watch): watching costs a thread and a stat per file (polling).
 * <p>
 * Selections add up: a data is watched when any selection matches. Only data with a source file are watched
 * (not placeholders nor data created by the code). Modifications and creations reload; a deleted file is ignored.
 */
public final class ResourceWatcher {
    public enum Mode {
        /** Compare date and size of the files each interval: works everywhere (default). */
        POLLING,
        /** Ask the OS (WatchService): cheaper, not recursive, not reliable on every file system. */
        SERVICE
    }

    public static final long DEFAULT_INTERVAL_MS = 1000;
    private static final long DEBOUNCE_MS = 200;
    private static final long REFRESH_MS = 1000;

    private final Resources resources;
    private final List<ResourceSelection> selections = new CopyOnWriteArrayList<>();
    private Mode mode = Mode.POLLING;
    private long intervalMs = DEFAULT_INTERVAL_MS;
    private FileChangeDetector detector;
    private volatile Map<Path, List<DataType>> dataByPath = Collections.emptyMap();
    private volatile int reloadCount;

    ResourceWatcher(Resources resources) {
        this.resources = resources;
    }

    /** Mode and interval used the next time the watcher starts (restarts it when running). */
    public synchronized void configure(Mode mode, long intervalMs) {
        boolean wasRunning = isRunning();
        stopDetector();
        this.mode = mode == null ? Mode.POLLING : mode;
        this.intervalMs = intervalMs > 0 ? intervalMs : DEFAULT_INTERVAL_MS;
        if (wasRunning)
            startDetector();
    }

    /** Watch this selection too; starts the watcher when it is not running. */
    public synchronized void watch(ResourceSelection selection) {
        selections.add(selection);
        if (!isRunning())
            startDetector();
    }

    public void watchAll() {
        watch(ResourceSelection.all());
    }

    public void watchType(Class<? extends DataType> type) {
        watch(ResourceSelection.ofType(type));
    }

    /** Stop watching and forget the selections. */
    public synchronized void stop() {
        stopDetector();
        selections.clear();
        dataByPath = Collections.emptyMap();
    }

    public synchronized boolean isRunning() {
        return detector != null;
    }

    public List<ResourceSelection> getSelections() {
        return new ArrayList<>(selections);
    }

    public int getReloadCount() {
        return reloadCount;
    }

    public synchronized String status() {
        if (!isRunning())
            return "Resource watcher off";

        return "Resource watcher " + mode + " every " + intervalMs + " ms, " + detector.trackedCount()
                + " files, " + reloadCount + " reloads, selections: " + selections;
    }

    private void startDetector() {
        detector = new FileChangeDetector(
                mode == Mode.SERVICE ? FileChangeDetector.Mode.WATCH_SERVICE : FileChangeDetector.Mode.POLLING,
                intervalMs, DEBOUNCE_MS, REFRESH_MS, this::selectedPaths, this::filesChanged);
        detector.start();
        System.out.println("--Watch resources: " + mode + " every " + intervalMs + " ms");
    }

    private void stopDetector() {
        if (detector == null)
            return;

        detector.stop();
        detector = null;
        System.out.println("--Watch resources: stopped");
    }

    // Watcher thread: the source files of the selected data that can be read again
    private Set<Path> selectedPaths() {
        Map<Path, List<DataType>> byPath = new HashMap<>();

        resources.forEachRegistered((type, data) -> {
            if (!data.isLoaded() && !data.hasFailed())
                return;
            if (!resources.isReloadable(data) || !matches(type, data))
                return;

            for (String source : data.sourcePaths()) {
                try {
                    Path path = Paths.get(source).toAbsolutePath().normalize();
                    byPath.computeIfAbsent(path, p -> new ArrayList<>(1)).add(data);
                } catch (InvalidPathException ignored) {
                }
            }
        });

        dataByPath = byPath;
        return byPath.keySet();
    }

    private boolean matches(Class<? extends DataType> type, DataType data) {
        for (ResourceSelection selection : selections) {
            if (selection.matches(type, data))
                return true;
        }
        return false;
    }

    // Watcher thread: Resources.reload hands over to the main thread
    private void filesChanged(List<Path> files) {
        Map<Path, List<DataType>> byPath = dataByPath;
        Set<DataType> toReload = new LinkedHashSet<>();
        for (Path file : files) {
            List<DataType> data = byPath.get(file);
            if (data != null)
                toReload.addAll(data);
        }

        for (DataType data : toReload) {
            System.out.println("--Watch: source of " + data.getClass().getSimpleName() + " " + data.getDataName()
                    + " changed, reloading");
            ++reloadCount;
            resources.reload(data);
        }
    }
}
