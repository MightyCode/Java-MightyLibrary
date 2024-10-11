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

    @Override
    public boolean load(int remainingMilliseconds) {
        super.load(remainingMilliseconds);

        this.shape.updateVbo(new float[]{
                -1, 1,
                -1, -1,
                1, -1,
                1, 1
        }, positionIndex);

        return true;
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

    public void unload(){
        unload(0);
    }

    @Override
    public void unload(int remainingMilliseconds) {
        super.unload(remainingMilliseconds);
        frameBuffer.unload();
    }
}