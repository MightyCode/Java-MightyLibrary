package io.github.mightycode.mightylib.graphics.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The objects of one kind (Mesh, Material) that exist and were not freed: weakly held, so that registering never
 * keeps an object alive. Read by the debug commands (gl materials, leak check). Without OpenGL; main thread only.
 */
public final class LiveObjects<T> {
    private final Map<T, Boolean> objects = Collections.synchronizedMap(new WeakHashMap<>());

    /** Called by the constructor of the object. */
    public void register(T object) {
        objects.put(object, Boolean.TRUE);
    }

    /** Called when the object is freed. */
    public void unregister(T object) {
        objects.remove(object);
    }

    /** The objects registered and still reachable, in no particular order. */
    public List<T> snapshot() {
        synchronized (objects) {
            return new ArrayList<>(objects.keySet());
        }
    }

    public int count() {
        return snapshot().size();
    }
}
