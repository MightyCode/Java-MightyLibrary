package io.github.mightycode.mightylib.graphics.renderer;

import io.github.mightycode.mightylib.graphics.shader.ShaderValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/*
 * Values of uniforms by name, sent at each draw by their location: each location is found once per program (the
 * sink and its program stamp), not at each draw. An optional value is sent only when the program has its uniform,
 * never reported missing. Main thread only.
 */
final class UniformValues {
    private static final int UNRESOLVED = -2;

    private static final class Entry {
        final ShaderValue value;
        final boolean optional;
        int location = UNRESOLVED;

        Entry(ShaderValue value, boolean optional) {
            this.value = value;
            this.optional = optional;
        }
    }

    // In the order they were added: sent in this order
    private final List<Entry> entries = new ArrayList<>();
    private final HashMap<String, Entry> byName = new HashMap<>();

    // The sink and program the locations belong to
    private IUniformSink resolvedFor;
    private long resolvedStamp;
    // Incremented by each put and remove: what depends on the names is checked again when it changes
    private int version;

    /** @return the previous value of this name, null when none */
    ShaderValue put(ShaderValue value, boolean optional) {
        ++version;
        Entry entry = new Entry(value, optional);
        Entry previous = byName.put(value.getName(), entry);
        if (previous != null) {
            entries.set(entries.indexOf(previous), entry);
            return previous.value;
        }

        entries.add(entry);
        return null;
    }

    boolean remove(String name) {
        Entry previous = byName.remove(name);
        if (previous == null)
            return false;

        ++version;
        entries.remove(previous);
        return true;
    }

    boolean has(String name) {
        return byName.containsKey(name);
    }

    ShaderValue get(String name) {
        Entry entry = byName.get(name);
        return entry == null ? null : entry.value;
    }

    boolean isOptional(String name) {
        Entry entry = byName.get(name);
        return entry != null && entry.optional;
    }

    int size() {
        return entries.size();
    }

    int version() {
        return version;
    }

    // Every value has one of these names
    boolean onlyNames(Set<String> names) {
        return onlyNames(names, Collections.emptySet());
    }

    // Every value is named in one of the two sets
    boolean onlyNames(Set<String> names, Set<String> others) {
        for (Entry entry : entries) {
            String name = entry.value.getName();
            if (!names.contains(name) && !others.contains(name))
                return false;
        }

        return true;
    }

    Collection<ShaderValue> values() {
        List<ShaderValue> values = new ArrayList<>(entries.size());
        for (Entry entry : entries)
            values.add(entry.value);
        return values;
    }

    /**
     * Sends every value to the current program of the sink, but those whose name is in skipped (values of the same
     * name sent by another set after this one). Each location is found once per program.
     */
    void send(IUniformSink sink, UniformValues skipped) {
        if (sink != resolvedFor || sink.programStamp() != resolvedStamp) {
            for (Entry entry : entries)
                entry.location = UNRESOLVED;
            resolvedFor = sink;
            resolvedStamp = sink.programStamp();
        }

        for (Entry entry : entries) {
            if (entry.location == UNRESOLVED) {
                entry.location = sink.location(entry.value.getName());
                if (entry.location == -1 && !entry.optional)
                    sink.reportMissing(entry.value.getName());
            }

            if (entry.location == -1)
                continue;

            if (skipped != null && skipped.byName.containsKey(entry.value.getName()))
                continue;

            sink.send(entry.location, entry.value);
        }
    }
}
