package MightyLibrary.mightylib.graphics.shape._2D;

import MightyLibrary.mightylib.main.WindowInfo;

public class VirtualSceneRenderer extends TextureRenderer {
    private final FrameBuffer frameBuffer;

    public VirtualSceneRenderer(WindowInfo info){
        super("postProcessing");
        frameBuffer = new FrameBuffer(info);
        this.shape.updateVbo(new float[]{
               -1, 1,
               -1, -1,
               1, -1,
               1, 1
        }, positionIndex);
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