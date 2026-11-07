package io.github.mightycode.mightylib.resources;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Tells when files change, on its own daemon thread. Not tied to the resources (see ResourceWatcher).
 * <ul>
 *   <li>POLLING (default): the date and size of every tracked file are compared each interval. Works everywhere,
 *   costs one stat per file and interval.</li>
 *   <li>WATCH_SERVICE: the OS tells which directories changed (java.nio WatchService). Cheaper, but one registration
 *   per directory (not recursive) and not reliable on every file system (network drives...): a directory that
 *   cannot be registered falls back to polling for its files.</li>
 * </ul>
 * A change is reported once the file stayed the same for the debounce time, because editors write in several steps.
 * A deleted file is ignored (the previous version is kept); a file created or restored is a change.
 */
final class FileChangeDetector {
    enum Mode { POLLING, WATCH_SERVICE }

    private static final class Stat {
        final long modified;
        final long size;

        Stat(long modified, long size) {
            this.modified = modified;
            this.size = size;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Stat && ((Stat) o).modified == modified && ((Stat) o).size == size;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(modified) * 31 + Long.hashCode(size);
        }
    }

    private static final class Pending {
        final Stat stat;
        final long since;

        Pending(Stat stat, long since) {
            this.stat = stat;
            this.since = since;
        }
    }

    private static final Stat MISSING = new Stat(-1, -1);

    private final Supplier<Set<Path>> pathSupplier;
    private final Consumer<List<Path>> onChanged;
    private final long intervalMs;
    private final long debounceMs;
    private final long refreshMs;
    private Mode mode;

    // Guarded by this
    private final Map<Path, Stat> baseline = new HashMap<>();
    private final Map<Path, Pending> pending = new HashMap<>();
    private final Set<Path> reportedMissing = new HashSet<>();
    private final Map<Path, WatchKey> registered = new HashMap<>();
    private Set<Path> tracked = Collections.emptySet();
    private Set<Path> polledInServiceMode = Collections.emptySet();
    private long lastRefresh;
    private boolean refreshed;

    private WatchService service;
    private volatile boolean running;
    private Thread thread;

    FileChangeDetector(Mode mode, long intervalMs, long debounceMs, long refreshMs,
                       Supplier<Set<Path>> pathSupplier, Consumer<List<Path>> onChanged) {
        this.mode = mode;
        this.intervalMs = Math.max(10, intervalMs);
        this.debounceMs = Math.max(0, debounceMs);
        this.refreshMs = Math.max(0, refreshMs);
        this.pathSupplier = pathSupplier;
        this.onChanged = onChanged;
    }

    Mode mode() {
        return mode;
    }

    synchronized void start() {
        if (running)
            return;

        if (mode == Mode.WATCH_SERVICE) {
            try {
                service = FileSystems.getDefault().newWatchService();
            } catch (IOException | UnsupportedOperationException e) {
                System.err.println("--Watch: no file system watch service (" + e.getMessage() + "), polling instead");
                mode = Mode.POLLING;
            }
        }

        running = true;
        thread = new Thread(this::loop, "ResourceFileWatcher");
        thread.setDaemon(true);
        thread.start();
    }

    void stop() {
        Thread toJoin;
        synchronized (this) {
            if (!running)
                return;

            running = false;
            toJoin = thread;
            thread = null;
            if (service != null) {
                try {
                    service.close();
                } catch (IOException ignored) {
                }
                service = null;
            }
        }

        toJoin.interrupt();
        try {
            toJoin.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void loop() {
        while (running) {
            Set<Path> events = null;
            try {
                long wait = nextWait();
                WatchService currentService;
                synchronized (this) {
                    currentService = service;
                }

                if (currentService != null)
                    events = waitEvents(currentService, wait);
                else
                    Thread.sleep(wait);
            } catch (InterruptedException | ClosedWatchServiceException e) {
                break;
            }

            try {
                List<Path> changed = tick(System.currentTimeMillis(), events);
                if (!changed.isEmpty())
                    onChanged.accept(changed);
            } catch (RuntimeException e) {
                System.err.println("--Watch: error while checking files: " + e);
            }
        }
    }

    // Sooner when a change waits for its debounce delay
    private synchronized long nextWait() {
        if (pending.isEmpty())
            return intervalMs;

        return Math.min(intervalMs, Math.max(20, debounceMs / 2));
    }

    // WATCH_SERVICE: the files reported by the system during the wait (an overflow means: check everything)
    private Set<Path> waitEvents(WatchService watchService, long waitMs) throws InterruptedException {
        Set<Path> events = new HashSet<>();
        WatchKey key = watchService.poll(waitMs, TimeUnit.MILLISECONDS);
        while (key != null) {
            Path directory = (Path) key.watchable();
            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.kind() == StandardWatchEventKinds.OVERFLOW)
                    return null;

                events.add(directory.resolve((Path) event.context()).toAbsolutePath().normalize());
            }
            key.reset();
            key = watchService.poll();
        }

        return events;
    }

    /**
     * One check. now: milliseconds. events: the files the system reported (WATCH_SERVICE), null to check all tracked
     * files (POLLING, or an overflow). Returns the files changed and stable since the last call.
     */
    synchronized List<Path> tick(long now, Set<Path> events) {
        if (!refreshed || now - lastRefresh >= refreshMs) {
            refresh(now);
            refreshed = true;
            lastRefresh = now;
        }

        Set<Path> toCheck;
        if (events == null || service == null && mode == Mode.POLLING) {
            toCheck = tracked;
        } else {
            toCheck = new HashSet<>(pending.keySet());
            toCheck.addAll(polledInServiceMode);
            for (Path event : events) {
                if (tracked.contains(event))
                    toCheck.add(event);
            }
        }

        List<Path> changed = new ArrayList<>();
        for (Path path : new ArrayList<>(toCheck))
            check(path, now, changed);

        return changed;
    }

    private void check(Path path, long now, List<Path> changed) {
        Stat current = stat(path);
        Stat known = baseline.get(path);

        if (current == MISSING) {
            pending.remove(path);
            if (known != MISSING && reportedMissing.add(path))
                System.out.println("--Watch: " + path + " no longer exists, previous version kept");
            return;
        }
        reportedMissing.remove(path);

        if (current.equals(known)) {
            pending.remove(path);
            return;
        }

        Pending wait = pending.get(path);
        if (wait == null || !wait.stat.equals(current)) {
            // Still changing: wait for it to stay the same
            pending.put(path, new Pending(current, now));
            wait = pending.get(path);
        }

        if (now - wait.since >= debounceMs) {
            pending.remove(path);
            baseline.put(path, current);
            changed.add(path);
        }
    }

    private void refresh(long now) {
        Set<Path> wanted = new HashSet<>(pathSupplier.get());

        for (Path path : wanted) {
            if (!baseline.containsKey(path))
                baseline.put(path, stat(path));
        }
        baseline.keySet().retainAll(wanted);
        pending.keySet().retainAll(wanted);
        reportedMissing.retainAll(wanted);
        tracked = wanted;

        if (service != null)
            updateRegistrations();
    }

    // WATCH_SERVICE: one registration per directory holding a tracked file
    private void updateRegistrations() {
        Set<Path> directories = new HashSet<>();
        for (Path path : tracked) {
            Path parent = path.getParent();
            if (parent != null)
                directories.add(parent);
        }

        for (Path directory : directories) {
            if (registered.containsKey(directory))
                continue;

            try {
                registered.put(directory, directory.register(service,
                        StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY));
            } catch (IOException | RuntimeException e) {
                registered.put(directory, null);
                System.err.println("--Watch: cannot watch " + directory + " (" + e + "), polling its files");
            }
        }

        registered.entrySet().removeIf(entry -> {
            if (directories.contains(entry.getKey()))
                return false;

            if (entry.getValue() != null)
                entry.getValue().cancel();
            return true;
        });

        Set<Path> polled = new HashSet<>();
        for (Path path : tracked) {
            Path parent = path.getParent();
            if (parent == null || registered.get(parent) == null)
                polled.add(path);
        }
        polledInServiceMode = polled;
    }

    private static Stat stat(Path path) {
        try {
            BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
            if (!attributes.isRegularFile())
                return MISSING;

            return new Stat(attributes.lastModifiedTime().toMillis(), attributes.size());
        } catch (IOException e) {
            return MISSING;
        }
    }

    synchronized int trackedCount() {
        return tracked.size();
    }

    synchronized int pendingCount() {
        return pending.size();
    }
}
