package io.github.mightycode.mightylib.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The order of creation of the services, decided once the plugins registered them: each service after those it
 * requires, and after those it declares "after" when they are registered. Among the services ready together, the order
 * of registration (the engine first). Pure: no engine, no GL.
 */
public final class ServiceOrder {
    private ServiceOrder() {}

    /**
     * Sorts and freezes the declarations (given in registration order).
     * @throws IllegalStateException if a required service is not registered, if two declarations have the same key, or
     *                               on a cycle: the message names the services
     */
    public static List<ServiceDeclaration<?>> Sort(List<ServiceDeclaration<?>> declarations) {
        int count = declarations.size();
        Map<ServiceKey<?>, Integer> indexes = new HashMap<>();
        for (int i = 0; i < count; ++i) {
            ServiceDeclaration<?> declaration = declarations.get(i);
            Integer previous = indexes.put(declaration.key(), i);
            if (previous != null)
                throw new IllegalStateException("Service " + declaration.key() + " declared twice (plugins "
                        + declarations.get(previous).plugin() + " and " + declaration.plugin() + ")");
        }

        // The services each one is created after, and the ones created after it
        List<Set<Integer>> dependencies = new ArrayList<>(count);
        List<List<Integer>> dependents = new ArrayList<>(count);
        for (int i = 0; i < count; ++i)
            dependents.add(new ArrayList<>());

        for (int i = 0; i < count; ++i) {
            ServiceDeclaration<?> declaration = declarations.get(i);
            Set<Integer> before = new LinkedHashSet<>();
            for (ServiceKey<?> required : declaration.requiredKeys()) {
                Integer index = indexes.get(required);
                if (index == null)
                    throw new IllegalStateException("Service " + declaration.key() + " of the plugin "
                            + declaration.plugin() + " requires " + required + ": no plugin registers it");
                before.add(index);
            }
            for (ServiceKey<?> previous : declaration.afterKeys()) {
                Integer index = indexes.get(previous);
                if (index != null)
                    before.add(index);
            }

            dependencies.add(before);
            for (int index : before)
                dependents.get(index).add(i);
        }

        // Kahn: each time, the first service in registration order whose dependencies are created
        int[] pending = new int[count];
        for (int i = 0; i < count; ++i)
            pending[i] = dependencies.get(i).size();

        boolean[] placed = new boolean[count];
        List<ServiceDeclaration<?>> sorted = new ArrayList<>(count);
        while (sorted.size() < count) {
            int next = -1;
            for (int i = 0; i < count && next < 0; ++i) {
                if (!placed[i] && pending[i] == 0)
                    next = i;
            }

            if (next < 0)
                throw new IllegalStateException("Cycle between the services: " + cycle(declarations, dependencies,
                        placed));

            placed[next] = true;
            sorted.add(declarations.get(next));
            for (int dependent : dependents.get(next))
                --pending[dependent];
        }

        for (ServiceDeclaration<?> declaration : sorted)
            declaration.freeze();

        return Collections.unmodifiableList(sorted);
    }

    // "a needs b needs a": each service left waits for another one left, so following them loops
    private static String cycle(List<ServiceDeclaration<?>> declarations, List<Set<Integer>> dependencies,
                                boolean[] placed) {
        List<Integer> path = new ArrayList<>();
        int current = 0;
        while (placed[current])
            ++current;

        while (!path.contains(current)) {
            path.add(current);
            for (int dependency : dependencies.get(current)) {
                if (!placed[dependency]) {
                    current = dependency;
                    break;
                }
            }
        }

        StringBuilder text = new StringBuilder();
        for (int i = path.indexOf(current); i < path.size(); ++i) {
            ServiceDeclaration<?> declaration = declarations.get(path.get(i));
            text.append(declaration.key()).append(" (plugin ").append(declaration.plugin()).append(") needs ");
        }

        return text.append(declarations.get(current).key()).toString();
    }
}
