package MightyLibrary.mightylib.graphics.renderer._2D;

import MightyLibrary.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import MightyLibrary.mightylib.main.WindowInfo;
import MightyLibrary.mightylib.graphics.surface.IGLBindable;

public class VirtualSceneRenderer extends RectangleRenderer {
    private final FrameBuffer frameBuffer;

    public VirtualSceneRenderer(WindowInfo info, IGLBindable bindable){
        super("postProcessing");
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