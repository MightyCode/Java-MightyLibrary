package io.github.mightycode.mightylib.resources;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
    // The changes of one data (description file + sources, written one after the other) make a single reload
    private static final long GROUP_MS = 300;

    private final Resources resources;
    private final List<ResourceSelection> selections = new CopyOnWriteArrayList<>();
    private Mode mode = Mode.POLLING;
    private long intervalMs = DEFAULT_INTERVAL_MS;
    private FileChangeDetector detector;
    private volatile Map<Path, List<DataType>> dataByPath = Collections.emptyMap();
    private volatile Set<Path> folders = Collections.emptySet();
    // Detector thread only: data changed, reloaded once nothing changed for GROUP_MS
    private final Map<DataType, Long> waiting = new LinkedHashMap<>();
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

        // The folders too: a file created in one is a new data (see filesChanged)
        Set<Path> watched = new HashSet<>(byPath.keySet());
        Set<Path> directories = selections.isEmpty() ? Collections.<Path>emptySet() : listFolders();
        watched.addAll(directories);
        folders = directories;
        return watched;
    }

    private Set<Path> listFolders() {
        Set<Path> directories = new HashSet<>();
        try {
            Files.walkFileTree(Paths.get(Resources.FOLDER).toAbsolutePath().normalize(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
                    directories.add(directory);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException e) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException | InvalidPathException e) {
            System.err.println("--Watch: can't list the resource folders (" + e + ")");
        }
        return directories;
    }

    private boolean matches(Class<? extends DataType> type, DataType data) {
        for (ResourceSelection selection : selections) {
            if (selection.matches(type, data))
                return true;
        }
        return false;
    }

    // Watcher thread, at every check (changes may be empty): Resources.reload hands over to the main thread
    private void filesChanged(List<Path> files) {
        long now = System.currentTimeMillis();
        Map<Path, List<DataType>> byPath = dataByPath;
        Set<Path> directories = folders;
        boolean foldersChanged = false;

        for (Path file : files) {
            if (directories.contains(file)) {
                foldersChanged = true;
                continue;
            }

            List<DataType> data = byPath.get(file);
            if (data != null) {
                for (DataType changed : data)
                    waiting.put(changed, now);
            }
        }

        Iterator<Map.Entry<DataType, Long>> entries = waiting.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<DataType, Long> entry = entries.next();
            if (now - entry.getValue() < GROUP_MS)
                continue;

            entries.remove();
            DataType data = entry.getKey();
            System.out.println("--Watch: source of " + data.getClass().getSimpleName() + " " + data.getDataName()
                    + " changed, reloading");
            ++reloadCount;
            resources.reload(data);
        }

        if (foldersChanged)
            resources.discoverNewFiles(this::matches);
    }
}
