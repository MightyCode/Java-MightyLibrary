package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.graphics.text.DrawnTexts;
import io.github.mightycode.mightylib.main.internal.FrameEvent;
import io.github.mightycode.mightylib.main.internal.TickEvent;
import io.github.mightycode.mightylib.main.utils.EngineStats;
import io.github.mightycode.mightylib.main.utils.GameTime;
import io.github.mightycode.mightylib.scenes.ISceneManager;
import io.github.mightycode.mightylib.utils.Logger;

public final class MainLoop {
    private final static float NANO_IN_SECOND = 1000000000.0f;

    // Deadline of the loading queues: the frame deadline under a loading screen, a share of the frame from now otherwise
    static long loadingDeadline(long now, long frameDeadline, double frameTime, float inGameShare, boolean loadingScreen) {
        if (loadingScreen)
            return frameDeadline;

        return Math.min(frameDeadline, now + (long) (frameTime * inGameShare));
    }

    // Process-wide: the engine mode is read by code that has no view (logs, debug tables)
    private static volatile EEngineMode mode = EEngineMode.RELEASE;
    public static EEngineMode getMode() { return mode; }
    public static boolean isDebug() { return mode == EEngineMode.DEBUG; }

    static void setMode(EEngineMode newMode) { mode = newMode; }

    private MainLoop() {}

    /**
     * Run the engine until the window is closed or a scene asks to exit, then free everything.
     * The JVM is not stopped: the application decides what to do with the returned status.
     *
     * @return ListError.NO_ERROR, a ListError start failure, or the status given by the scene
     */
    public static int run(EngineConfig config) {
        Engine engine = new Engine(config);

        // The engine is the current view outside the scenes: start, commands, loading
        try (Views.Scope ignored = Views.enter(engine)) {
            int setupStatus = engine.setup();
            if (setupStatus != ListError.NO_ERROR)
                return engine.shutdown(setupStatus);

            int status = loop(engine);
            return engine.shutdown(status);
        }
    }

    // One tick: time, then update of the engine. Returns the CPU time spent (nanoseconds).
    // Called by the loop and by the test kit, so that both run the same code.
    static long tick(Engine engine) {
        TickEvent tickEvent = new TickEvent();
        tickEvent.begin();
        long tickStart = System.nanoTime();
        GameTime.update();
        engine.update();
        long tickWork = System.nanoTime() - tickStart;
        EngineStats.endTick(tickWork);
        tickEvent.commit();
        return tickWork;
    }

    // One frame (headless: no display, no GL, no swap): display, then loading queues, main thread tasks and GL elements until the deadline
    // (System.nanoTime, at most one frame from now), then buffer swap. Returns the CPU time spent before the swap.
    // Called by the loop and by the test kit, so that both run the same code.
    static long frame(Engine engine, long deadline) {
        EngineConfig config = engine.config();
        double frameTime = NANO_IN_SECOND / config.getFps();

        FrameEvent frameEvent = new FrameEvent();
        frameEvent.begin();
        long frameStart = System.nanoTime();
        // Without window (Engine.setupHeadless): no display, the GL elements stay in the sink
        boolean windowed = !engine.isHeadless();
        DrawnTexts.beginFrame();
        if (windowed) {
            long renderStart = System.nanoTime();
            engine.displayWindows();
            EngineStats.countRenderNanos(System.nanoTime() - renderStart);

            // What follows (loading queues, main thread tasks, screenshots) works in the main window
            engine.contexts().requireMainWindow().getWindow().bindWindow();
        }

        // GL elements are sent with the time left in this frame, before the next tick or frame.
        // No tick is due now (the tick branch would have run), at least one step is done anyway.
        long frameDeadline = Math.min(deadline, frameStart + (long) frameTime);
        // The loading uses the whole frame under a loading screen, a share of it while the scene is displayed.
        // Every queue does at least one step, so the loading always progresses; the main thread tasks
        // (posted by any code, not only the loading) keep the whole frame.
        long loadingDeadline = loadingDeadline(System.nanoTime(), frameDeadline, frameTime,
                config.getInGameLoadingShare(), engine.isAnySceneLoading());

        engine.resources().processUntil(loadingDeadline);
        MainThread.process(frameDeadline);
        if (windowed)
            engine.glResources().process(loadingDeadline);

        // Measured before the buffer swap, which can wait for the GPU
        long frameWork = System.nanoTime() - frameStart;
        EngineStats.endFrame(frameStart, frameWork);
        frameEvent.commit();

        if (windowed) {
            long swapStart = System.nanoTime();
            engine.swapWindows();
            EngineStats.endSwap(System.nanoTime() - swapStart);
        }
        return frameWork;
    }

    // Return the status of the end of the loop
    @SuppressWarnings("BusyWait")
    private static int loop(Engine engine) {
        EngineConfig config = engine.config();
        ISceneManager sceneManager = engine.sceneManager();

        double tickTime = NANO_IN_SECOND / config.getTps();
        double frameTime = NANO_IN_SECOND / config.getFps();

        // Set loop parameters
        int ticks = 0;
        int frames = 0;

        // CPU time spent in update and display, to measure optimizations independently of the FPS cap
        long tickNanos = 0;
        long frameNanos = 0;

        double lastTick = 0.0;
        double lastFrame = 0.0;
        double lastSecond = 0.0;

        long start = System.nanoTime();

        Window window = engine.contexts().requireMainWindow().getWindow();

        while (!engine.isStopRequested() && !sceneManager.isExitRequested()) {
            long now = System.nanoTime() - start;
            if (now - lastTick >= tickTime) {
                tickNanos += tick(engine);
                ++ticks;

                while (now - lastTick >= tickTime)
                    lastTick += tickTime;
            } else if (now - lastFrame >= frameTime) {
                frameNanos += frame(engine, start + (long) (lastTick + tickTime));
                ++frames;
                while (now - lastFrame >= frameTime)
                    lastFrame += frameTime;
            }

            if (now - lastSecond >= NANO_IN_SECOND) {
                EngineStats.endSecond(frames, ticks, frameNanos, tickNanos);

                if (isDebug()) {
                    window.setTitle(String.format("%s | FPS:%d; TPS:%d | Frame: %.2f ms; Tick: %.2f ms",
                            config.getProjectName(), EngineStats.getFps(), EngineStats.getTps(),
                            EngineStats.getFrameMs(), EngineStats.getTickMs()));
                }

                ticks = frames = 0;
                tickNanos = frameNanos = 0;
                lastSecond += NANO_IN_SECOND;
            }

            double remaining = Math.min(tickTime - (now - lastTick), frameTime - (now - lastFrame)) / 1e6 * 0.7f;
            if (remaining > 1) {
                try {
                    Thread.sleep((long)remaining);
                } catch (InterruptedException e) {
                    // Nothing interrupts the main thread on purpose: the loop goes on with the next frame
                    Logger.Warn(Logger.CATEGORY_ENGINE, "Main loop sleep interrupted");
                }
            }
        }

        return sceneManager.isExitRequested() ? sceneManager.getExitStatus() : ListError.NO_ERROR;
    }
}
