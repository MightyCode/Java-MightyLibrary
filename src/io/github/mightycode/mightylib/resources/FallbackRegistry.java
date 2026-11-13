package io.github.mightycode.mightylib.resources;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The placeholder data of each type (ResourceLoader.createFallback), used instead of a data that is missing, still
 * loading or failed.
 */
class FallbackRegistry {
    // Reference keeping the fallbacks loaded for the whole program
    private static final String FALLBACK_REFERENCE = "fallback";

    private final Resources resources;
    // Fallback by loader type, filled by register then only read
    private final Map<Class<? extends DataType>, DataType> fallbacks = new ConcurrentHashMap<>();
    // Unknown names already reported by getResourceOrFallback (main thread)
    private final Set<String> reportedUnknownNames = new HashSet<>();

    FallbackRegistry(Resources resources) {
        this.resources = resources;
    }

    void register(ResourceLoader loader, Map<String, DataType> map) {
        DataType fallback = loader.createFallback();
        if (fallback == null)
            return;

        if (!Resources.FALLBACK_NAME.equals(fallback.getDataName()))
            throw new IllegalStateException("The fallback of " + loader.getResourceNameType() + " must be named " + Resources.FALLBACK_NAME);

        if (map.containsKey(Resources.FALLBACK_NAME))
            System.err.println("--The " + loader.getResourceNameType() + " named " + Resources.FALLBACK_NAME
                    + " is replaced by the fallback of its type");

        fallback.setNotManuallyCreated();
        fallback.attachTo(resources);
        map.put(Resources.FALLBACK_NAME, fallback);
        fallbacks.put(loader.getType(), fallback);
    }

    // Referenced for the whole program: loaded first, unloaded at the exit
    void loadAll() {
        resources.runOnMainThread(() -> {
            for (DataType fallback : fallbacks.values())
                resources.addReferenceAndUpdate(fallback, FALLBACK_REFERENCE);
        });
    }

    // Fallback of the type, or of its closest parent type with a loader; null without fallback
    <T extends DataType> T get(Class<T> type) {
        DataType fallback = fallbackOf(type);
        return type.isInstance(fallback) ? type.cast(fallback) : null;
    }

    // A ColorTextureData uses the TextureData fallback
    private DataType fallbackOf(Class<?> type) {
        for (Class<?> current = type; current != null && DataType.class.isAssignableFrom(current); current = current.getSuperclass()) {
            DataType fallback = fallbacks.get(current);
            if (fallback != null)
                return fallback;
        }

        return null;
    }

    boolean has(DataType data) {
        return fallbackOf(data.getClass()) != null;
    }

    boolean is(DataType data) {
        return data != null && fallbackOf(data.getClass()) == data;
    }

    /**
     * What to use for this data now: the data itself when loaded, otherwise the fallback of the type
     * (data failed, still loading or null), or the data as is when the type has no fallback.
     */
    <T extends DataType> T usable(Class<T> type, T data) {
        if (data != null && data.isLoaded())
            return data;

        T fallback = get(type);
        return fallback != null ? fallback : data;
    }

    // The fallback of the type for an unknown name (reported once per name), or a failure in strict mode
    <T extends DataType> T forUnknown(Class<T> type, String name, boolean strict) {
        T fallback = get(type);
        String key = type.getSimpleName() + " " + name;
        if (strict)
            throw new IllegalStateException("Strict resources: unknown " + key);

        if (reportedUnknownNames.add(key))
            System.err.println("--Unknown " + key + (fallback != null ? ", its fallback is used" : ""));

        return fallback;
    }
}
