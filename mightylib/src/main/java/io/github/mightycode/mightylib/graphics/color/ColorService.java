package io.github.mightycode.mightylib.graphics.color;

import io.github.mightycode.mightylib.main.MainThread;
import io.github.mightycode.mightylib.resources.EngineResources;

/**
 * The color service of the engine: the default color space of the draws, given at the start
 * (EngineConfig.setColorSpace, -Dmightylib.colorSpace), the final pass of the engine (EngineResources.POST_PROCESSING)
 * and its settings, then changed by the code or the commands. No GL.
 */
public final class ColorService implements IColorService {
    // Written on the main thread, read when the views draw
    private volatile ERenderColorSpace colorSpace;
    private volatile String finalPassShader = EngineResources.POST_PROCESSING;
    private volatile PictureSettings picture = PictureSettings.DEFAULT;

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
        checkMainThread();
        this.colorSpace = checked;
    }

    @Override
    public String finalPassShader() {
        return finalPassShader;
    }

    @Override
    public void setFinalPassShader(String shaderName) {
        if (shaderName == null || shaderName.isEmpty())
            throw new IllegalArgumentException("The final pass needs the name of a shader");
        checkMainThread();
        finalPassShader = shaderName;
    }

    @Override
    public PictureSettings picture() {
        return picture;
    }

    @Override
    public void setPicture(PictureSettings picture) {
        if (picture == null)
            throw new IllegalArgumentException("The settings of the picture can't be null");
        checkMainThread();
        this.picture = picture;
    }

    private static void checkMainThread() {
        if (!MainThread.isMainThread())
            throw new IllegalStateException("The colors of the engine change on the main thread");
    }
}
