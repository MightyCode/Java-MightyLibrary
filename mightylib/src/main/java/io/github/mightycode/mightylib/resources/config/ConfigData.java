package io.github.mightycode.mightylib.resources.config;

import io.github.mightycode.mightylib.main.MainThread;
import io.github.mightycode.mightylib.main.Workers;
import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.SingleSourceDataType;
import io.github.mightycode.mightylib.utils.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A configuration in layers, configs/<name>.json: the defaults of every source (engine, game, mods) merged key by key
 * (objects merged deeply, the highest source wins), then the layer of the player (the source of its data folder,
 * ResourceSources.PLAYER_OWNER), the only one written: it holds only the keys the player changed.
 *
 * <p>Read with typed getters and a default ("gamma", or "a.b" for a key of an object). set and remove change the
 * player's layer at once and write it on a worker (a temporary file, then renamed). Watched like the other resources:
 * a file changed on the disk reads the data again (getVersion changes). A layer that can't be read is ignored with a
 * line naming its file: the values of the other layers stay. Main thread.
 */
public final class ConfigData extends SingleSourceDataType {
    // Filled by the loader while the data is read, before it is loaded
    private JSONObject defaults = new JSONObject();
    private JSONObject player = new JSONObject();
    private JSONObject merged = new JSONObject();
    private List<String> layers = List.of();
    private List<String> invalidLayers = List.of();
    private boolean invalidPlayerLayer;
    // Null: no layer of the player (nothing is written)
    private Path playerFile;

    // The content to write and its version; the worker writes only the latest one
    private final AtomicReference<PendingWrite> pending = new AtomicReference<>();
    private final Object writeLock = new Object();
    private long writtenVersion;
    private long requestedVersion;

    public ConfigData(String name) {
        super(TYPE_SET_UP.THREAD_CONTEXT, name, PathOf(name));
    }

    // Logical path of the configuration of this name
    public static String PathOf(String name) {
        return ConfigDataLoader.FOLDER + "/" + name + ".json";
    }

    @Override
    protected DataType createReloadCopy() {
        return new ConfigData(getDataName());
    }

    @Override
    protected void swapContent(DataType copy) {
        ConfigData other = (ConfigData) copy;
        JSONObject otherDefaults = other.defaults;
        other.defaults = defaults;
        defaults = otherDefaults;

        JSONObject otherPlayer = other.player;
        other.player = player;
        player = otherPlayer;

        JSONObject otherMerged = other.merged;
        other.merged = merged;
        merged = otherMerged;

        List<String> otherLayers = other.layers;
        other.layers = layers;
        layers = otherLayers;

        List<String> otherInvalid = other.invalidLayers;
        other.invalidLayers = invalidLayers;
        invalidLayers = otherInvalid;

        boolean otherInvalidPlayer = other.invalidPlayerLayer;
        other.invalidPlayerLayer = invalidPlayerLayer;
        invalidPlayerLayer = otherInvalidPlayer;

        Path otherFile = other.playerFile;
        other.playerFile = playerFile;
        playerFile = otherFile;
    }

    // Called by the loader (worker) with what it read
    void setRead(JSONObject defaults, JSONObject player, List<String> layers, List<String> invalidLayers,
                 boolean invalidPlayerLayer, Path playerFile) {
        this.defaults = defaults;
        this.player = player;
        this.layers = List.copyOf(layers);
        this.invalidLayers = List.copyOf(invalidLayers);
        this.invalidPlayerLayer = invalidPlayerLayer;
        this.playerFile = playerFile;
    }

    @Override
    protected boolean internLoad() {
        merged = Merge(defaults, player);
        return true;
    }

    @Override
    protected void internUnload() {
        defaults = new JSONObject();
        player = new JSONObject();
        merged = new JSONObject();
        layers = List.of();
        invalidLayers = List.of();
        invalidPlayerLayer = false;
    }

    // ---- Reading ----

    public boolean has(String key) {
        return Find(merged, key) != null;
    }

    public float getFloat(String key, float defaultValue) {
        Object value = Find(merged, key);
        return value instanceof Number ? ((Number) value).floatValue() : defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        Object value = Find(merged, key);
        return value instanceof Number ? ((Number) value).intValue() : defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = Find(merged, key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    public String getString(String key, String defaultValue) {
        Object value = Find(merged, key);
        return value instanceof String ? (String) value : defaultValue;
    }

    // Copies: changing them changes nothing in the data
    public JSONObject values() {
        return Copy(merged);
    }

    public JSONObject playerValues() {
        return Copy(player);
    }

    // The defaults of the sources, without the player's layer
    public JSONObject defaultValues() {
        return Copy(defaults);
    }

    // The file of the player's layer, existing or not; null without layer of the player
    public Path playerFile() {
        return playerFile;
    }

    // The sources read by the last reading, from the lowest to the highest
    public List<String> layers() {
        return layers;
    }

    // The files that could not be read by the last reading (their values are missing)
    public List<String> invalidLayers() {
        return invalidLayers;
    }

    // ---- Writing (player's layer) ----

    /**
     * Sets a value of the player (a number, a boolean or a string; "a.b" in an object) and writes the player's layer.
     * The future completes once the file is written (at once when nothing is written).
     *
     * @throws IllegalArgumentException for another type of value
     */
    public CompletableFuture<Void> set(String key, Object value) {
        if (!(value instanceof Number || value instanceof Boolean || value instanceof String))
            throw new IllegalArgumentException("A value of configuration is a number, a boolean or a string, not "
                    + (value == null ? "null" : value.getClass().getSimpleName()));
        checkMainThread();

        String[] parts = Parts(key);
        JSONObject parent = player;
        for (int i = 0; i < parts.length - 1; ++i) {
            Object child = parent.opt(parts[i]);
            if (!(child instanceof JSONObject)) {
                child = new JSONObject();
                parent.put(parts[i], child);
            }
            parent = (JSONObject) child;
        }
        parent.put(parts[parts.length - 1], value);

        return changed();
    }

    // Removes a value of the player (the default of the sources comes back) and writes the player's layer
    public CompletableFuture<Void> remove(String key) {
        checkMainThread();

        String[] parts = Parts(key);
        JSONObject parent = player;
        for (int i = 0; i < parts.length - 1 && parent != null; ++i)
            parent = parent.optJSONObject(parts[i]);
        if (parent == null || parent.remove(parts[parts.length - 1]) == null)
            return CompletableFuture.completedFuture(null);

        return changed();
    }

    private CompletableFuture<Void> changed() {
        merged = Merge(defaults, player);

        if (playerFile == null) {
            System.err.println("--Config " + getDataName() + ": no layer of the player, the change is not saved");
            return CompletableFuture.completedFuture(null);
        }
        if (invalidPlayerLayer) {
            System.err.println("--Config " + getDataName() + ": " + playerFile + " can't be read, the change is not"
                    + " saved (fix or delete the file)");
            return CompletableFuture.completedFuture(null);
        }

        pending.set(new PendingWrite(++requestedVersion, playerFile, player.toString(4)));
        return Workers.run(this::writeLatest);
    }

    // On a worker: the latest content only, never an older one after a newer one
    private void writeLatest() {
        synchronized (writeLock) {
            PendingWrite write = pending.get();
            if (write == null || write.version <= writtenVersion)
                return;

            try {
                WriteAtomically(write.file, write.content);
            } catch (IOException e) {
                Logger.LogException("Can't write the configuration " + write.file, e);
            }
            writtenVersion = write.version;
        }
    }

    // A temporary file next to it, renamed: the file is never half written
    static void WriteAtomically(Path file, String content) throws IOException {
        Path absolute = file.toAbsolutePath();
        Path parent = absolute.getParent();
        Path name = absolute.getFileName();
        if (parent == null || name == null)
            throw new IOException("A configuration file can't be a root: " + file);
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, name.toString(), ".tmp");
        try {
            Files.write(temporary, content.getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void checkMainThread() {
        if (!MainThread.isMainThread())
            throw new IllegalStateException("The configuration " + getDataName() + " changes on the main thread");
    }

    @Override
    protected void describeDetails(JSONObject details) {
        details.put("layers", new JSONArray(layers));
        if (!invalidLayers.isEmpty())
            details.put("invalidLayers", new JSONArray(invalidLayers));
        details.put("playerFile", playerFile == null ? JSONObject.NULL : playerFile.toString());
        details.put("player", Copy(player));
        details.put("values", Copy(merged));
    }

    // ---- JSON ----

    private static String[] Parts(String key) {
        if (key == null || key.isEmpty() || key.startsWith(".") || key.endsWith(".") || key.contains(".."))
            throw new IllegalArgumentException("Invalid key of configuration ->" + key + "<-");

        return key.split("\\.");
    }

    // The value at this key ("a.b" in an object), null if absent
    static Object Find(JSONObject object, String key) {
        String[] parts = Parts(key);
        JSONObject parent = object;
        for (int i = 0; i < parts.length - 1; ++i) {
            parent = parent.optJSONObject(parts[i]);
            if (parent == null)
                return null;
        }

        Object value = parent.opt(parts[parts.length - 1]);
        return value == JSONObject.NULL ? null : value;
    }

    // The layers from the lowest to the highest, merged deeply in a new object: an object merges with an object,
    // any other value replaces the lower one
    static JSONObject Merge(JSONObject... layers) {
        JSONObject merged = new JSONObject();
        for (JSONObject layer : layers)
            MergeInto(merged, layer);

        return merged;
    }

    static void MergeInto(JSONObject into, JSONObject layer) {
        Iterator<String> keys = layer.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = layer.get(key);
            Object current = into.opt(key);
            if (value instanceof JSONObject && current instanceof JSONObject)
                MergeInto((JSONObject) current, (JSONObject) value);
            else
                into.put(key, CopyValue(value));
        }
    }

    static JSONObject Copy(JSONObject object) {
        return Merge(object);
    }

    private static Object CopyValue(Object value) {
        if (value instanceof JSONObject)
            return Copy((JSONObject) value);
        if (value instanceof JSONArray)
            return new JSONArray(((JSONArray) value).toList());

        return value;
    }

    private static final class PendingWrite {
        final long version;
        final Path file;
        final String content;

        PendingWrite(long version, Path file, String content) {
            this.version = version;
            this.file = file;
            this.content = content;
        }
    }
}
