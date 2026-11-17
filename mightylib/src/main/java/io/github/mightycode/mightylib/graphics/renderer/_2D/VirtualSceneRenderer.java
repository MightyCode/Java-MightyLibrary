package io.github.mightycode.mightylib.graphics.renderer._2D;

import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.main.WindowInfo;
import io.github.mightycode.mightylib.graphics.surface.IGLBindable;
import io.github.mightycode.mightylib.resources.EngineResources;

public final class VirtualSceneRenderer extends RectangleRenderer {
    private final FrameBuffer frameBuffer;

    public VirtualSceneRenderer(WindowInfo info, IGLBindable bindable){
        super(EngineResources.POST_PROCESSING);
        frameBuffer = new FrameBuffer(bindable, info.getVirtualSizeRef().x, info.getVirtualSizeRef().y);
    }

    // The whole window, in normalized device coordinates
    @Override
    protected float[] calculatePosition() {
        return new float[]{
                -1, 1,
                -1, -1,
                1, -1,
                1, 1
        };
    }

    public FrameBuffer getFrameBuffer(){
        return frameBuffer;
    }


    public void bindFrameBuff(){
        frameBuffer.bindFrameBuffer();
    }


    public void unbindFrameBuff(){
        frameBuffer.unbindFrameBuffer();
        frameBuffer.bindRenderTexture();
    }

    @Override
    public void unload() {
        super.unload();
        frameBuffer.unload();
    }
}