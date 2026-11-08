package io.github.mightycode.mightylib.resources;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Links between the data: a wanted data references its dependencies (so they are loaded without being listed) and
 * waits for them to be ready before its own load. Main thread only.
 */
class DependencyGraph {
    private final Resources resources;
    // Data waiting for their dependencies (the urgent ones are counted for finishedCurrentLoading)
    private final Set<DataType> waiting = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    DependencyGraph(Resources resources) {
        this.resources = resources;
    }

    int waitingCount() {
        return waiting.size();
    }

    // Waiting data wanted now: those only preloaded do not hold a loading screen
    int waitingUrgentCount() {
        int count = 0;
        for (DataType data : waiting) {
            if (resources.isUrgent(data))
                ++count;
        }

        return count;
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

    /**
     * Path of a dependency cycle going through the data ("A -> B -> A"), or null. Such data would wait for each other
     * forever. A reload copy is checked as the data it replaces (its dependencies are the same).
     */
    static String findCycle(DataType data) {
        DataType root = data.reloadOf != null ? data.reloadOf : data;
        List<DataType> path = new ArrayList<>();
        path.add(root);

        if (!reaches(root, root, path, new HashSet<>()))
            return null;

        StringBuilder text = new StringBuilder();
        for (DataType step : path) {
            if (text.length() > 0)
                text.append(" -> ");
            text.append(step.getClass().getSimpleName()).append(' ').append(step.getDataName());
        }
        return text.toString();
    }

    // Depth-first search of target among the dependencies of current; path ends with target when found
    private static boolean reaches(DataType current, DataType target, List<DataType> path, Set<DataType> visited) {
        for (DataType dependency : current.getDependencies()) {
            if (dependency == target) {
                path.add(dependency);
                return true;
            }

            if (!visited.add(dependency))
                continue;

            path.add(dependency);
            if (reaches(dependency, target, path, visited))
                return true;
            path.remove(path.size() - 1);
        }

        return false;
    }

    // A wanted data references its dependencies (known once preloaded), so they are loaded without being listed
    void acquire(DataType data) {
        for (DataType dependency : data.getDependencies()) {
            if (!data.acquiredDependencies.contains(dependency)) {
                data.acquiredDependencies.add(dependency);
                addDependent(dependency, data);
                resources.addReferenceAndUpdate(dependency, data.dependencyReference());
                if (data.urgent || (data.reloadOf != null && data.reloadOf.urgent))
                    resources.makeUrgent(dependency);
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
            waiting.add(data);
        }

        for (DataType dependency : data.getDependencies()) {
            if (!isReady(dependency) && !dependency.waiters.contains(data))
                dependency.waiters.add(data);
        }
    }

    void stopWaiting(DataType data) {
        if (data.waitingDependencies) {
            data.waitingDependencies = false;
            waiting.remove(data);
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
