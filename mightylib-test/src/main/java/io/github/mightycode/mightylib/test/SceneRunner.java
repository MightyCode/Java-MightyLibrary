package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.inputs.inputType.ActionInput;
import io.github.mightycode.mightylib.inputs.IInputManager;
import io.github.mightycode.mightylib.inputs.InputAction;
import io.github.mightycode.mightylib.inputs.internal.InputSimulation;
import io.github.mightycode.mightylib.scenes.ISceneManager;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.SceneRegistry;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import org.json.JSONObject;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Drives the scenes and the inputs of an engine with a window (TestEngine.scenes()). The inputs go through the same
 * calls as the input command: they are read by the next tick, so run frames after them.
 * Leaks: leakSnapshot before, assertNoLeak after, both on the empty scene (toEmptyScene).
 */
public final class SceneRunner {
    private final TestEngine engine;

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
     * resources by type, and the sound sources owned. Measured once the urgent GL elements are loaded.
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
