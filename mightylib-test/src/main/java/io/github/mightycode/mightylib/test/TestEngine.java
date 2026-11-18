package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.main.EWindowMode;
import io.github.mightycode.mightylib.main.Engine;
import io.github.mightycode.mightylib.main.EngineConfig;
import io.github.mightycode.mightylib.main.KitEngineAccess;
import io.github.mightycode.mightylib.main.ListError;
import io.github.mightycode.mightylib.main.MainThread;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.main.utils.KitTimeAccess;
import io.github.mightycode.mightylib.registry.EngineRegistry;
import io.github.mightycode.mightylib.registry.IPlugin;
import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An engine driven by a test: started from the configuration of the game (same plugins), it only runs when the test
 * calls step. Its time is manual: each step moves it by one tick (1 / TPS), advance moves it further.
 * Created by MightyTest; close it (try-with-resources) to stop it and reset the process-wide state of the engine.
 * Main thread = the thread that started it: call it from that thread only.
 */
public final class TestEngine implements AutoCloseable {
    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    // The engine has a process-wide state (main thread, workers, time): one test engine at a time
    private static final AtomicBoolean RUNNING = new AtomicBoolean();
    // An engine with a window kept between the tests (@MightySceneTest): closed when another engine starts
    private static TestEngine shared;
    // The configurations already given to a started engine (weak: forgotten with them)
    private static final Set<EngineConfig> STARTED_CONFIGS = Collections.synchronizedSet(
            Collections.newSetFromMap(new WeakHashMap<>()));

    // Namespace of the plugin that stacks the folders of the test above the sources of the game
    static final String SOURCES_NAMESPACE = "mightytest";

    private final EngineConfig config;
    private final boolean headless;
    private final List<Path> sources = new ArrayList<>();
    private final RecordingGLSink sink = new RecordingGLSink();
    private boolean strictResources = true;

    private Engine engine;
    private Views.Scope scope;
    private SceneRunner runner;
    private long clock;
    private boolean owner;
    private boolean closed;

    TestEngine(EngineConfig config, boolean headless) {
        this.config = config;
        this.headless = headless;
    }

    /** A folder of resources above the sources of the game (its files replace theirs), before start. */
    public TestEngine addSource(Path folder) {
        if (engine != null)
            throw new IllegalStateException("The sources are added before start");

        sources.add(folder);
        return this;
    }

    /**
     * Before start, for a test of a failing resource: a failure gives the fallback (as in the game) instead of
     * throwing. The resources are strict by default.
     */
    public TestEngine allowResourceFailures() {
        if (engine != null)
            throw new IllegalStateException("The resources are made lenient before start");

        strictResources = false;
        return this;
    }

    /**
     * Starts the engine as the game does, with strict resources, the silent sound, neither standard input nor debug
     * server, then loads the resources.
     * @throws IllegalStateException if another test engine runs, or if the engine can't start (the error is named)
     */
    public TestEngine start() {
        if (engine != null || closed)
            throw new IllegalStateException("A test engine starts once");
        // The kit changes the configuration (plugin of the test sources, start scene, display): one per engine
        if (!STARTED_CONFIGS.add(config))
            throw new IllegalStateException("This EngineConfig already started a test engine: give a new one to each"
                    + " engine (a Supplier<EngineConfig>, as the JUnit annotations do)");
        TestEngine idle = shared;
        if (idle != null && idle != this)
            idle.close();
        if (!RUNNING.compareAndSet(false, true))
            throw new IllegalStateException("Another test engine runs: close it first (one engine per JVM at a time)");
        owner = true;

        if (!sources.isEmpty())
            config.addPlugin(new SourcesPlugin(sources));

        if (!headless) {
            // Hidden, on a scene without resources: the tests start the scenes they need
            KitEngineAccess.setHiddenWindows(true);
            config.setStartScene(EmptyTestScene::new);
            // Never the fullscreen of the game (it would switch the screen of the machine), no wait for the screen
            config.setDisplay(config.getDisplay().setMode(EWindowMode.WINDOWED).setVsync(false));
        }

        clock = NANOS_PER_SECOND;
        KitTimeAccess.setClock(() -> clock);

        int status;
        try {
            engine = KitEngineAccess.create(config, strictResources);
            scope = Views.enter(engine);
            status = headless ? KitEngineAccess.setupHeadless(engine, sink) : KitEngineAccess.setup(engine);
        } catch (RuntimeException | Error e) {
            // A strict resource that failed, a plugin that threw: the next test starts from a clean state
            close();
            throw e;
        }
        if (status != ListError.NO_ERROR) {
            close();
            if (status == ListError.LIBRARIES_LOAD_FAIL)
                throw new GLUnavailableException("GLFW can't start: no screen (display server) here");
            if (status == ListError.WINDOW_CREATION_FAIL)
                throw new GLUnavailableException("No window with an OpenGL 3.3 core context here");
            throw new IllegalStateException("The engine did not start: error " + status + " (ListError)");
        }

        return this;
    }

    /** One tick then one frame, without waiting: the time moves by one tick first. */
    public TestEngine step() {
        requireStarted();
        clock += Math.round((double) NANOS_PER_SECOND / config.getTps());
        KitEngineAccess.tick(engine);
        KitEngineAccess.frame(engine, Long.MAX_VALUE);
        return this;
    }

    public TestEngine step(int count) {
        for (int i = 0; i < count; i++)
            step();
        return this;
    }

    /** Frames only (no tick), the time does not move: loading, GL elements, display. */
    public TestEngine frames(int count) {
        requireStarted();
        for (int i = 0; i < count; i++)
            KitEngineAccess.frame(engine, Long.MAX_VALUE);
        return this;
    }

    /** Moves the time without running the engine: the next tick sees this much more time (GameTime.DeltaTime). */
    public TestEngine advance(float seconds) {
        if (seconds < 0)
            throw new IllegalArgumentException("The time only moves forward");

        clock += Math.round(seconds * (double) NANOS_PER_SECOND);
        return this;
    }

    /**
     * Steps until no data is being loaded nor any scene, no main thread task waits, and (with a window) no urgent GL
     * element waits.
     */
    public TestEngine loadUntilDone() {
        return loadUntilDone(Duration.ofSeconds(30));
    }

    /** @throws AssertionError if the loading is not done before the timeout (real time) */
    public TestEngine loadUntilDone(Duration timeout) {
        requireStarted();
        long end = System.nanoTime() + timeout.toNanos();
        while (!isLoadingDone()) {
            if (System.nanoTime() > end)
                throw new AssertionError("Loading not done after " + timeout.toMillis() + " ms");
            step();
        }

        return this;
    }

    private boolean isLoadingDone() {
        return engine.resources().finishedCurrentLoading() && MainThread.pendingTasks() == 0
                && !KitEngineAccess.isAnySceneLoading(engine)
                && (headless || !engine.glResources().hasElementsToLoad());
    }

    /** Runs a command as the game's code would (Commands.execute), for the engine as its view. */
    public CommandResult execute(String line) {
        requireStarted();
        return engine.commands().execute(line);
    }

    public Engine engine() {
        requireStarted();
        return engine;
    }

    public Resources resources() {
        return engine().resources();
    }

    /**
     * Headless: the data whose GL element was asked (by a batch or a fallback), with the number of times.
     * Nothing is sent to a graphic card. Empty with a window (the GL elements are real: see the gl command).
     */
    public Map<DataType, Integer> glElementRequests() {
        return sink.references();
    }

    /** A resource that fails throws (default), or gives its fallback (allowResourceFailures). */
    public boolean isStrictResources() {
        return strictResources;
    }

    // The configuration this engine started from (the kit added the plugin of the test sources)
    public EngineConfig config() {
        return config;
    }

    boolean isHeadless() {
        return headless;
    }

    boolean isClosed() {
        return closed;
    }

    // Kept between tests by the JUnit extension: the next engine to start closes it first
    void markShared() {
        shared = this;
    }

    /** With a window: the driver of its scenes and inputs. */
    public SceneRunner scenes() {
        requireStarted();
        if (headless)
            throw new IllegalStateException("A headless engine has no scene: MightyTest.withWindow");
        if (runner == null)
            runner = new SceneRunner(this);
        return runner;
    }

    /** The time of the manual clock, in seconds (GameTime.currentTime reads it). */
    public float time() {
        return (float) ((double) clock / NANOS_PER_SECOND);
    }

    void requireStarted() {
        if (engine == null || closed)
            throw new IllegalStateException("The test engine is not started (start) or is closed");
    }

    /** Stops the engine, frees everything, resets the process-wide state. Can be called twice. */
    @Override
    public void close() {
        if (closed)
            return;
        closed = true;

        try {
            if (engine != null)
                KitEngineAccess.shutdown(engine, ListError.NO_ERROR);
        } finally {
            try {
                if (scope != null)
                    scope.close();
                KitEngineAccess.resetProcessState();
                KitTimeAccess.reset();
            } finally {
                // Whatever failed above, the next test can start an engine
                if (shared == this)
                    shared = null;
                if (owner)
                    RUNNING.set(false);
            }
        }
    }

    // The folders of the test, above the sources of the game and of its plugins (registered last)
    private static final class SourcesPlugin implements IPlugin {
        private final List<Path> folders;

        SourcesPlugin(List<Path> folders) {
            this.folders = Collections.unmodifiableList(new ArrayList<>(folders));
        }

        @Override
        public String namespace() {
            return SOURCES_NAMESPACE;
        }

        @Override
        public void register(EngineRegistry registry) {
            for (Path folder : folders)
                registry.resources().folder(folder.toAbsolutePath().toString());
        }
    }
}
