package io.github.mightycode.mightylib.registry;

import java.util.Objects;

/**
 * A single value the engine sets and any later plugin replaces (a factory of the engine): the last one wins, each
 * replacement reported in one line.
 */
final class Replaceable<V> {
    // "Scene manager factory"...: first words of the message
    private final String kind;
    private V value;
    private String plugin;

    Replaceable(String kind) {
        this.kind = kind;
    }

    void set(String plugin, V value) {
        Objects.requireNonNull(value, kind);
        if (this.value != null)
            System.out.println("--" + kind + " of " + this.plugin + " replaced by " + plugin);

        this.value = value;
        this.plugin = plugin;
    }

    // Null if nothing set it
    V get() {
        return value;
    }

    // The plugin that set the value, null if none
    String plugin() {
        return plugin;
    }
}
