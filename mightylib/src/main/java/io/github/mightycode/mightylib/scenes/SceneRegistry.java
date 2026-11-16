package io.github.mightycode.mightylib.scenes;

import io.github.mightycode.mightylib.registry.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * The scenes of an engine by name (Engine.scenes), registered by the plugins (EngineRegistry.scenes): what the command
 * scene and the menus of a game offer. Optional: the code can always create a scene that is not registered.
 * A name is the full identifier ("demo:scene2d") or the path alone ("scene2d", case ignored) when no other scene has
 * it. Frozen once built.
 */
public final class SceneRegistry {
    public static final class Entry {
        private final Identifier identifier;
        private final Class<? extends Scene> type;
        private final Supplier<? extends Scene> factory;
        private final String description;

        private Entry(Identifier identifier, Class<? extends Scene> type, Supplier<? extends Scene> factory,
                      String description) {
            this.identifier = identifier;
            this.type = type;
            this.factory = factory;
            this.description = description;
        }

        public Identifier identifier() { return identifier; }

        // The class of the scenes the factory creates
        public Class<? extends Scene> type() { return type; }

        // What the scene shows, for a menu or the help
        public String description() { return description; }

        // A new scene: created under the current view, like any scene created by code
        public Scene create() {
            return Objects.requireNonNull(factory.get(), "The factory of the scene " + identifier + " gave null");
        }

        @Override
        public String toString() { return identifier.toString(); }
    }

    private final Map<Identifier, Entry> entries = new LinkedHashMap<>();
    private boolean frozen;

    /**
     * The plugins register through EngineRegistry.scenes, which checks that the factory creates the given class.
     * @throws IllegalArgumentException if the identifier is registered, or the description is empty
     */
    public SceneRegistry declare(Identifier identifier, Class<? extends Scene> type,
                                 Supplier<? extends Scene> factory, String description) {
        if (frozen)
            throw new IllegalStateException("The scenes are frozen: register " + identifier
                    + " in IPlugin.register (registry.scenes())");
        CheckEntry(identifier, type, factory, description);
        if (entries.containsKey(identifier))
            throw new IllegalArgumentException("The scene " + identifier + " is already registered");

        entries.put(identifier, new Entry(identifier, type, factory, description));
        return this;
    }

    /**
     * @throws IllegalArgumentException if a part is missing or the description is empty
     */
    public static void CheckEntry(Identifier identifier, Class<? extends Scene> type, Supplier<?> factory,
                                  String description) {
        Objects.requireNonNull(identifier, "identifier");
        Objects.requireNonNull(type, "type of the scene " + identifier);
        Objects.requireNonNull(factory, "factory of the scene " + identifier);
        if (description == null || description.isEmpty())
            throw new IllegalArgumentException("The scene " + identifier + " needs a description");
    }

    // Nothing can be registered afterward
    public SceneRegistry freeze() {
        frozen = true;
        return this;
    }

    // In registration order
    public List<Entry> entries() {
        return List.copyOf(entries.values());
    }

    /**
     * The scenes matching the name: the full identifier, or else the path alone (case ignored) in any namespace,
     * sorted by identifier. One = found, several = ambiguous, none = unknown.
     */
    public List<Entry> match(String name) {
        List<Entry> result = new ArrayList<>();
        if (name == null)
            return result;

        if (name.indexOf(':') >= 0) {
            try {
                Entry entry = entries.get(Identifier.parse(name.toLowerCase(Locale.ROOT)));
                if (entry != null)
                    result.add(entry);
            } catch (IllegalArgumentException e) {
                // Not a valid identifier : nothing matches
            }
            return result;
        }

        for (Entry entry : entries.values()) {
            if (entry.identifier.path().equalsIgnoreCase(name))
                result.add(entry);
        }

        result.sort(Comparator.comparing(Entry::identifier));
        return result;
    }

    // The only scene matching the name, null if unknown or ambiguous
    public Entry find(String name) {
        List<Entry> found = match(name);
        return found.size() == 1 ? found.get(0) : null;
    }

    // The first scene registered with this class, null if none: the code may create a scene that is not registered
    public Entry entryOf(Class<?> type) {
        for (Entry entry : entries.values()) {
            if (entry.type == type)
                return entry;
        }

        return null;
    }

    // The names to type: the path alone, the full identifier when another namespace has the same path; sorted
    public List<String> completions() {
        Map<String, Integer> pathCount = new HashMap<>();
        for (Identifier identifier : entries.keySet())
            pathCount.merge(identifier.path(), 1, Integer::sum);

        List<String> names = new ArrayList<>();
        for (Identifier identifier : entries.keySet())
            names.add(pathCount.get(identifier.path()) > 1 ? identifier.toString() : identifier.path());

        Collections.sort(names);
        return names;
    }
}
