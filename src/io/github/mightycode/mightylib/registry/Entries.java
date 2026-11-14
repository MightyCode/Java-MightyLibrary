package io.github.mightycode.mightylib.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

/**
 * The entries of one kind (commands, loaders...), each with the plugin that registered it, in registration order.
 * A key is registered once; a later plugin replaces it explicitly (replace keeps its place).
 */
final class Entries<K, V> {
    private static final class Entry<V> {
        private final String plugin;
        private final V value;

        private Entry(String plugin, V value) {
            this.plugin = plugin;
            this.value = value;
        }
    }

    // "Command", "Loader of"...: first words of the messages
    private final String kind;
    private final Function<K, String> keyName;
    private final LinkedHashMap<K, Entry<V>> entries = new LinkedHashMap<>();

    Entries(String kind, Function<K, String> keyName) {
        this.kind = kind;
        this.keyName = keyName;
    }

    /**
     * @throws IllegalStateException if the key is already registered: the message names both plugins
     */
    void register(String plugin, K key, V value) {
        Entry<V> existing = entries.get(key);
        if (existing != null) {
            throw new IllegalStateException(kind + " " + keyName.apply(key) + " registered by " + existing.plugin
                    + " and " + plugin + " (a later plugin can replace it)");
        }

        entries.put(key, new Entry<>(plugin, value));
    }

    /**
     * Replaces the entry of a previous plugin (another namespace and the engine included), reported in one line.
     * @throws IllegalStateException if nothing is registered with this key
     */
    void replace(String plugin, K key, V value) {
        Entry<V> existing = entries.get(key);
        if (existing == null)
            throw new IllegalStateException(kind + " " + keyName.apply(key) + " can't be replaced by " + plugin
                    + ": nothing registered it");

        entries.put(key, new Entry<>(plugin, value));
        System.out.println("--" + kind + " " + keyName.apply(key) + " of " + existing.plugin + " replaced by "
                + plugin);
    }

    // In registration order (a replaced entry keeps its place)
    List<V> values() {
        List<V> values = new ArrayList<>(entries.size());
        for (Entry<V> entry : entries.values())
            values.add(entry.value);

        return values;
    }

    // What the plugin registered or replaced, in registration order
    List<V> valuesOf(String plugin) {
        List<V> values = new ArrayList<>();
        for (Entry<V> entry : entries.values()) {
            if (entry.plugin.equals(plugin))
                values.add(entry.value);
        }

        return values;
    }

    // Null if the key is not registered
    V get(K key) {
        Entry<V> entry = entries.get(key);
        return entry == null ? null : entry.value;
    }
}
