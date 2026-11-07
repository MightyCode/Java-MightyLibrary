package io.github.mightycode.mightylib.resources;

import java.util.ArrayList;
import java.util.List;

/**
 * Links between the data: a wanted data references its dependencies (so they are loaded without being listed) and
 * waits for them to be ready before its own load. Main thread only.
 */
class DependencyGraph {
    private final Resources resources;
    // Data waiting for their dependencies (counted for finishedCurrentLoading)
    private int waitingCount;

    DependencyGraph(Resources resources) {
        this.resources = resources;
    }

    int waitingCount() {
        return waitingCount;
    }

    // Loaded (or replaced by its fallback) and not reading a new version: computed once, with the latest one
    private boolean isReady(DataType dependency) {
        return (dependency.isLoaded() && dependency.reloadCopy == null) || (dependency.failed && resources.hasFallback(dependency));
    }

    boolean allReady(DataType data) {
        for (DataType dependency : data.getDependencies()) {
            if (!isReady(dependency))
                return false;
        }

        return true;
    }

    // A wanted data references its dependencies (known once preloaded), so they are loaded without being listed
    void acquire(DataType data) {
        for (DataType dependency : data.getDependencies()) {
            if (!data.acquiredDependencies.contains(dependency)) {
                data.acquiredDependencies.add(dependency);
                addDependent(dependency, data);
                resources.addReferenceAndUpdate(dependency, data.dependencyReference());
            }
        }
    }

    void release(DataType data) {
        if (data.acquiredDependencies.isEmpty())
            return;

        List<DataType> released = new ArrayList<>(data.acquiredDependencies);
        data.acquiredDependencies.clear();
        for (DataType dependency : released) {
            dependency.dependents.remove(data);
            resources.removeReferenceAndUpdate(dependency, data.dependencyReference());
        }
    }

    // A copy is not told: it waits for its dependencies to finish their own reload (isReady)
    static void addDependent(DataType dependency, DataType dependent) {
        if (dependent.reloadOf == null && !dependency.dependents.contains(dependent))
            dependency.dependents.add(dependent);
    }

    void waitFor(DataType data) {
        if (!data.waitingDependencies) {
            data.waitingDependencies = true;
            ++waitingCount;
        }

        for (DataType dependency : data.getDependencies()) {
            if (!isReady(dependency) && !dependency.waiters.contains(data))
                dependency.waiters.add(data);
        }
    }

    void stopWaiting(DataType data) {
        if (data.waitingDependencies) {
            data.waitingDependencies = false;
            --waitingCount;
        }
    }

    void wakeWaiters(DataType loaded) {
        for (DataType waiter : loaded.waiters) {
            if (waiter.waitingDependencies)
                resources.enqueueUpdate(waiter);
        }

        loaded.waiters.clear();
    }
}
