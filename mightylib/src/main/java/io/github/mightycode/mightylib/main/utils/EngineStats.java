package io.github.mightycode.mightylib.main.utils;

import org.json.JSONObject;

/**
 * Rendering counters and timings of the last full second.
 * Counters are incremented by the engine on the main thread, they cost a single addition.
 */
public final class EngineStats {
    private EngineStats() {}

    // Counters of the current second
    private static long drawCalls;
    private static long programBinds;
    private static long uniformUploads;
    private static long textureBinds;
    private static long glStateChanges;
    private static long glStateChangesSkipped;
    private static long bufferUploadBytes;
    // CPU time of the windows display (Engine.displayWindows), without the buffer swap
    private static long renderNanos;

    // Values of the last full second
    private static int fps;
    private static int tps;
    private static double frameMs;
    private static double tickMs;
    private static double drawCallsPerFrame;
    private static double programBindsPerFrame;
    private static double uniformUploadsPerFrame;
    private static double textureBindsPerFrame;
    private static double glStateChangesPerFrame;
    private static double glStateChangesSkippedPerFrame;
    private static double bufferUploadBytesPerSecond;
    private static double renderCpuMs;

    // Worst frames since the last resetMax, to check that a loading screen stays fluid
    private static long lastFrameStart = -1;
    private static double maxFrameMs;
    private static double maxFrameIntervalMs;

    // Called by the main loop after each frame: frameStart is System.nanoTime at its start
    public static void endFrame(long frameStart, long workNanos) {
        maxFrameMs = Math.max(maxFrameMs, workNanos / 1e6);
        if (lastFrameStart >= 0)
            maxFrameIntervalMs = Math.max(maxFrameIntervalMs, (frameStart - lastFrameStart) / 1e6);

        lastFrameStart = frameStart;
    }

    private static double maxTickMs;
    private static double maxSwapMs;

    // Buffer swap: waits for the GPU (vsync, uploads still processed by the driver)
    public static void endSwap(long swapNanos) {
        maxSwapMs = Math.max(maxSwapMs, swapNanos / 1e6);
    }

    // Called by the main loop after each tick
    public static void endTick(long workNanos) {
        maxTickMs = Math.max(maxTickMs, workNanos / 1e6);
    }

    public static void resetMax() {
        // The next interval would include what happened before the reset
        lastFrameStart = -1;
        maxFrameMs = 0;
        maxFrameIntervalMs = 0;
        maxTickMs = 0;
        maxSwapMs = 0;
    }

    public static void countDrawCall() { ++drawCalls; }
    public static void countProgramBind() { ++programBinds; }
    public static void countUniformUpload() { ++uniformUploads; }
    public static void countTextureBind() { ++textureBinds; }
    // An OpenGL state change sent, or skipped because the context already had it (GLStateCache)
    public static void countGLStateChange() { ++glStateChanges; }
    public static void countGLStateSkipped() { ++glStateChangesSkipped; }
    public static void countBufferUpload(long bytes) { bufferUploadBytes += bytes; }
    // Called by the main loop around Engine.displayWindows
    public static void countRenderNanos(long nanos) { renderNanos += nanos; }

    // Called once per second by the main loop
    public static void endSecond(int frames, int ticks, long frameNanos, long tickNanos) {
        fps = frames;
        tps = ticks;
        frameMs = frames == 0 ? 0 : frameNanos / 1e6 / frames;
        tickMs = ticks == 0 ? 0 : tickNanos / 1e6 / ticks;

        double frameCount = Math.max(1, frames);
        drawCallsPerFrame = drawCalls / frameCount;
        programBindsPerFrame = programBinds / frameCount;
        uniformUploadsPerFrame = uniformUploads / frameCount;
        textureBindsPerFrame = textureBinds / frameCount;
        glStateChangesPerFrame = glStateChanges / frameCount;
        glStateChangesSkippedPerFrame = glStateChangesSkipped / frameCount;
        bufferUploadBytesPerSecond = bufferUploadBytes;
        renderCpuMs = renderNanos / 1e6 / frameCount;
        renderNanos = 0;

        drawCalls = programBinds = uniformUploads = textureBinds = bufferUploadBytes = glStateChanges = glStateChangesSkipped = 0;
    }

    public static int getFps() { return fps; }
    public static int getTps() { return tps; }
    public static double getFrameMs() { return frameMs; }
    public static double getTickMs() { return tickMs; }

    public static JSONObject toJson() {
        Runtime runtime = Runtime.getRuntime();

        JSONObject json = new JSONObject();
        json.put("fps", fps);
        json.put("tps", tps);
        json.put("frameMs", round(frameMs));
        json.put("tickMs", round(tickMs));
        json.put("maxFrameMsSinceReset", round(maxFrameMs));
        json.put("maxFrameIntervalMsSinceReset", round(maxFrameIntervalMs));
        json.put("maxTickMsSinceReset", round(maxTickMs));
        json.put("maxSwapMsSinceReset", round(maxSwapMs));
        json.put("renderCpuMs", round(renderCpuMs));
        json.put("drawCallsPerFrame", round(drawCallsPerFrame));
        json.put("programBindsPerFrame", round(programBindsPerFrame));
        json.put("uniformUploadsPerFrame", round(uniformUploadsPerFrame));
        json.put("textureBindsPerFrame", round(textureBindsPerFrame));
        json.put("glStateChangesPerFrame", round(glStateChangesPerFrame));
        json.put("glStateChangesSkippedPerFrame", round(glStateChangesSkippedPerFrame));
        json.put("bufferUploadBytesPerSecond", (long) bufferUploadBytesPerSecond);
        json.put("usedMemoryMB", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
        json.put("maxMemoryMB", runtime.maxMemory() / (1024 * 1024));

        return json;
    }

    private static double round(double value) {
        return Math.round(value * 1000) / 1000.0;
    }
}
