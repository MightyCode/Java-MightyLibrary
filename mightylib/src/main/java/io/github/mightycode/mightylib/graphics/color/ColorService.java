package io.github.mightycode.mightylib.graphics.color;

import io.github.mightycode.mightylib.main.MainThread;

/**
 * The color service of the engine: the default color space of the draws, given at the start
 * (EngineConfig.setColorSpace, -Dmightylib.colorSpace) then changed by the code or the commands. No GL.
 */
public final class ColorService implements IColorService {
    // Written on the main thread, read when the draws resolve their mode
    private volatile ERenderColorSpace colorSpace;

    public ColorService(ERenderColorSpace colorSpace) {
        this.colorSpace = ERenderColorSpace.CheckEngine(colorSpace);
    }

    @Override
    public ERenderColorSpace colorSpace() {
        return colorSpace;
    }

    @Override
    public void setColorSpace(ERenderColorSpace colorSpace) {
        ERenderColorSpace checked = ERenderColorSpace.CheckEngine(colorSpace);
        if (!MainThread.isMainThread())
            throw new IllegalStateException("The color space of the engine changes on the main thread");

        this.colorSpace = checked;
    }
}
