package MightyLibrary.mightylib.graphics.renderer._2D;

import MightyLibrary.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import MightyLibrary.mightylib.graphics.renderer._2D.shape.Shape2DRenderer;
import MightyLibrary.mightylib.main.WindowInfo;
import MightyLibrary.mightylib.resources.texture.IGLBindable;

public class VirtualSceneRenderer extends RectangleRenderer {
    private final FrameBuffer frameBuffer;

    public VirtualSceneRenderer(WindowInfo info, IGLBindable bindable){
        super("postProcessing");
        frameBuffer = new FrameBuffer(bindable, info.getVirtualSizeRef().x, info.getVirtualSizeRef().y);
    }

    @Override
    public Shape2DRenderer init() {
        super.init();

        this.shape.updateVbo(new float[]{
                -1, 1,
                -1, -1,
                1, -1,
                1, 1
        }, positionIndex);

        return this;
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
        super.unload();
        frameBuffer.unload();
    }
}