package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.resources.internal.IGLElementSink;
import io.github.mightycode.mightylib.resources.texture.ETextureUpload;
import io.github.mightycode.mightylib.resources.texture.TextureUploader;
import io.github.mightycode.mightylib.utils.Logger;
import org.apiguardian.api.API;

import static org.apiguardian.api.API.Status.INTERNAL;

/** Bridge of the test kit to the package-private start, loop and process-wide state of the engine. Not for the games. */
@API(status = INTERNAL)
public final class KitEngineAccess {
    private KitEngineAccess() {}

    // An engine run by the test kit: silent sound, neither standard input nor debug server, strict or lenient resources
    public static Engine create(EngineConfig config, boolean strictResources) {
        Engine engine = new Engine(config);
        engine.setUnderTest(strictResources);
        return engine;
    }

    public static boolean isStrictResources(Engine engine) {
        return engine.isStrictResources();
    }

    // ListError.NO_ERROR or the error of the start
    public static int setupHeadless(Engine engine, IGLElementSink sink) {
        return engine.setupHeadless(sink);
    }

    public static int setup(Engine engine) {
        return engine.setup();
    }

    public static int shutdown(Engine engine, int status) {
        return engine.shutdown(status);
    }

    public static void tick(Engine engine) {
        MainLoop.tick(engine);
    }

    public static void frame(Engine engine, long deadlineNanos) {
        MainLoop.frame(engine, deadlineNanos);
    }

    public static boolean isStopRequested(Engine engine) {
        return engine.isStopRequested();
    }

    public static boolean isAnySceneLoading(Engine engine) {
        return engine.isAnySceneLoading();
    }

    // Before the setup of an engine with windows: they are created hidden
    public static void setHiddenWindows(boolean hidden) {
        Window.setHiddenWindows(hidden);
    }

    // Once the engine is stopped: the process-wide state back to the one before any engine
    public static void resetProcessState() {
        Window.setHiddenWindows(false);
        MainThread.reset();
        MainLoop.setMode(EEngineMode.RELEASE);
        Logger.setGLErrorCheck(true);
        TextureUploader.setMode(ETextureUpload.DIRECT);
    }
}
