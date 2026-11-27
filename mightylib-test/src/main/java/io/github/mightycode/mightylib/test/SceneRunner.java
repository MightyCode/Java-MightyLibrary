package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.graphics.utils.KitGraphicsAccess;
import io.github.mightycode.mightylib.inputs.inputType.ActionInput;
import io.github.mightycode.mightylib.inputs.IInputManager;
import io.github.mightycode.mightylib.inputs.InputAction;
import io.github.mightycode.mightylib.inputs.internal.InputSimulation;
import io.github.mightycode.mightylib.main.Window;
import io.github.mightycode.mightylib.scenes.ISceneManager;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.SceneRegistry;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import org.joml.Vector2i;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Drives the scenes and the inputs of an engine with a window (TestEngine.scenes()). The inputs go through the same
 * calls as the input command: they are read by the next tick, so run frames after them.
 * Leaks: leakSnapshot before, assertNoLeak after, both on the empty scene (toEmptyScene).
 */
public final class SceneRunner {
    private final TestEngine engine;
    // The driver reads the image of a hidden window back (checked at the first capture)
    private boolean readBackChecked;

    SceneRunner(TestEngine engine) {
        this.engine = engine;
    }

    public TestEngine engine() {
        return engine;
    }

    private ISceneManager manager() {
        engine.requireStarted();
        return engine.engine().sceneManager();
    }

    // ---- Scenes ----

    /** Asks for the scene; it is created and loaded by the next frames (untilLoaded). */
    public SceneRunner start(Supplier<? extends Scene> scene, String... args) {
        manager().requestSceneChange(scene.get(), args);
        return this;
    }

    /**
     * A scene registered by the plugins, by its identifier or its unique name (as the scene command).
     * @throws IllegalArgumentException if no scene or several have this name
     */
    public SceneRunner start(String name, String... args) {
        List<SceneRegistry.Entry> matches = engine.engine().scenes().match(name);
        if (matches.size() != 1)
            throw new IllegalArgumentException((matches.isEmpty() ? "No scene " : "Ambiguous scene ") + name
                    + (matches.isEmpty() ? "" : ": " + matches));

        manager().requestSceneChange(matches.get(0).create(), args);
        return this;
    }

    /** Steps until the scene asked is loaded, its resources and urgent GL elements too. */
    public SceneRunner untilLoaded() {
        return untilLoaded(Duration.ofSeconds(30));
    }

    public SceneRunner untilLoaded(Duration timeout) {
        long end = System.nanoTime() + timeout.toNanos();
        do {
            if (System.nanoTime() > end)
                throw new AssertionError("Scene " + describeScene() + " not loaded after " + timeout.toMillis() + " ms");
            engine.step();
        } while (manager().isSceneChangePending() || manager().isCurrentSceneLoading());

        engine.loadUntilDone(Duration.ofNanos(Math.max(1, end - System.nanoTime())));
        return this;
    }

    /** Back to the empty scene, loaded: what the previous scene held is released. */
    public SceneRunner toEmptyScene() {
        return start(EmptyTestScene::new).untilLoaded();
    }

    public Scene scene() {
        return manager().getCurrentScene();
    }

    private String describeScene() {
        Scene scene = manager().getCurrentScene();
        return scene == null ? "(none)" : scene.getClass().getSimpleName();
    }

    /** Ticks then frames (TestEngine.step): the inputs simulated before are read by the first tick. */
    public SceneRunner frames(int count) {
        engine.step(count);
        return this;
    }

    public CommandResult execute(String line) {
        return engine.execute(line);
    }

    // ---- Images ----

    /**
     * Displays one frame (the time does not move) and gives the final image of the main window of the scene: what
     * the window shows, read before its buffers are swapped.
     * @throws AssertionError if the driver does not read the image of a hidden window back, if the window was not
     * displayed or is minimized, or if its image has not the size of the window (scale of the screen)
     */
    public CapturedImage capture() {
        Scene scene = scene();
        if (scene == null)
            throw new IllegalStateException("No scene to capture: start one first");
        return capture(scene.context().getWindow());
    }

    /** The final image of this window (a secondary window of a context), after one frame. */
    public CapturedImage capture(Window window) {
        engine.requireStarted();
        if (!window.getInfo().isWindowCreated())
            throw new IllegalStateException("The window " + window.getInfo().getWindowName() + " is not created");

        if (!readBackChecked) {
            if (!KitGraphicsAccess.readsBackBuffer(window))
                throw new AssertionError("This driver does not read the image of a hidden window back (pixel"
                        + " ownership): the images of the tests can't be captured here");
            readBackChecked = true;
        }

        CompletableFuture<KitGraphicsAccess.WindowImage> request = KitGraphicsAccess.requestImage(window);
        boolean displayed = false;
        try {
            engine.frames(1);
            displayed = request.isDone();
        } finally {
            // Not displayed (or the frame failed): the request is not left for a later display
            if (!request.isDone())
                KitGraphicsAccess.cancelImageRequests(window);
        }
        if (!displayed)
            throw new AssertionError("The window " + window.getInfo().getWindowName() + " was not displayed by the frame");

        KitGraphicsAccess.WindowImage image;
        try {
            image = request.join();
        } catch (CompletionException e) {
            throw new AssertionError("No image of the window " + window.getInfo().getWindowName() + ": "
                    + e.getCause().getMessage(), e.getCause());
        }

        Vector2i size = window.getInfo().getSizeCopy();
        if (image.width() != size.x || image.height() != size.y)
            throw new AssertionError("The image of the window is " + image.width() + "x" + image.height()
                    + " for a window of " + size.x + "x" + size.y + " (scale of the screen?): compare at a scale of 1");

        return CapturedImage.of(image);
    }

    // ---- Inputs of the main window ----

    public IInputManager inputs() {
        engine.requireStarted();
        return engine.engine().contexts().requireMainWindow().getInputManager();
    }

    private InputAction action(String name) {
        List<ActionInput> matches = inputs().matchActions(name);
        if (matches.size() != 1)
            throw new IllegalArgumentException((matches.isEmpty() ? "No action " : "Ambiguous action ") + name
                    + ": " + matches.stream().map(match -> match.action().toString()).collect(Collectors.toList()));
        return matches.get(0).action();
    }

    /** Presses the action during ticks (from the next tick). */
    public SceneRunner press(InputAction action, int ticks) {
        inputs().simulatePress(action, ticks);
        return this;
    }

    /** By its identifier or unique name, as the input command. */
    public SceneRunner press(String action, int ticks) {
        return press(action(action), ticks);
    }

    public SceneRunner hold(InputAction action) {
        return press(action, InputSimulation.UNTIL_RELEASE);
    }

    public SceneRunner hold(String action) {
        return hold(action(action));
    }

    public SceneRunner release(InputAction action) {
        inputs().simulateRelease(action);
        return this;
    }

    public SceneRunner release(String action) {
        return release(action(action));
    }

    /** A key (GLFW code) during ticks. */
    public SceneRunner key(int key, int ticks) {
        inputs().getKeyboardManager().simulateKey(key, ticks);
        return this;
    }

    public SceneRunner keyHold(int key) {
        return key(key, InputSimulation.UNTIL_RELEASE);
    }

    public SceneRunner keyRelease(int key) {
        inputs().getKeyboardManager().simulateKeyRelease(key);
        return this;
    }

    /** Typed text, read by KeyboardManager.getTypedText at the next tick. */
    public SceneRunner text(String text) {
        inputs().getKeyboardManager().simulateText(text);
        return this;
    }

    /** Moves the cursor (window coordinates), kept until the real cursor moves. */
    public SceneRunner mouse(float x, float y) {
        inputs().getMouseManager().simulateMove(x, y);
        return this;
    }

    /** Clicks the button (GLFW code) during one tick at this place. */
    public SceneRunner click(int button, float x, float y) {
        mouse(x, y);
        inputs().getMouseManager().simulateButton(button, 1);
        return this;
    }

    /** Positive y is up. */
    public SceneRunner scroll(float x, float y) {
        inputs().getMouseManager().simulateScroll(x, y);
        return this;
    }

    // ---- Leaks ----

    /**
     * The OpenGL objects alive and the references of the shared GL elements (names without their #id), the referenced
     * resources by type, the sound sources owned, and the contexts and windows open (windows command). Measured once the
     * urgent GL elements are loaded.
     */
    public Map<String, Integer> leakSnapshot() {
        engine.loadUntilDone();
        Map<String, Integer> counters = new TreeMap<>();

        JSONObject gl = data("gl");
        JSONObject objects = gl.optJSONObject("glObjects");
        if (objects != null) {
            for (String name : objects.keySet())
                counters.put("gl." + name, objects.getInt(name));
        }
        JSONObject retained = gl.optJSONObject("retainedObjects");
        if (retained != null) {
            for (String name : retained.keySet())
                counters.put("retained." + name, retained.getInt(name));
        }
        JSONObject shared = gl.optJSONObject("sharedElements");
        if (shared != null) {
            for (String kind : shared.keySet()) {
                JSONObject elements = shared.optJSONObject(kind);
                if (elements == null)
                    continue;
                for (String name : elements.keySet()) {
                    Object references = elements.get(name);
                    if (references instanceof Number)
                        counters.merge(kind + "." + name.replaceAll("#[0-9]+$", ""), ((Number) references).intValue(),
                                Integer::sum);
                }
            }
        }

        JSONObject types = engine.resources().describe().optJSONObject("types");
        if (types != null) {
            for (String type : types.keySet())
                counters.put("resources." + type + ".referenced", types.getJSONObject(type).optInt("referenced"));
        }

        counters.put("sound.ownedSources", data("sounds").optInt("owned"));

        // The windows and contexts a scene opened and did not close (names of the contexts as the windows command)
        JSONArray contexts = data("windows").optJSONArray("contexts");
        if (contexts != null) {
            counters.put("windows.contexts", contexts.length());
            for (int i = 0; i < contexts.length(); ++i) {
                JSONObject context = contexts.getJSONObject(i);
                JSONArray windows = context.optJSONArray("windows");
                counters.merge("windows." + context.optString("name") + ".windows", windows == null ? 0 : windows.length(),
                        Integer::sum);
            }
        }
        return counters;
    }

    private JSONObject data(String command) {
        CommandResult result = engine.execute(command);
        if (!result.isSuccess())
            throw new IllegalStateException("The command " + command + " failed: " + result.getMessage());
        JSONObject data = result.getData();
        return data == null ? new JSONObject() : data;
    }

    /** @throws AssertionError naming every counter that changed since the snapshot */
    public void assertNoLeak(Map<String, Integer> before) {
        Map<String, Integer> after = leakSnapshot();
        TreeSet<String> keys = new TreeSet<>(before.keySet());
        keys.addAll(after.keySet());
        List<String> changes = keys.stream()
                .filter(key -> !before.getOrDefault(key, 0).equals(after.getOrDefault(key, 0)))
                .map(key -> key + " " + before.getOrDefault(key, 0) + "->" + after.getOrDefault(key, 0))
                .collect(Collectors.toList());
        if (!changes.isEmpty())
            throw new AssertionError("Leak: " + String.join(", ", changes));
    }
}
