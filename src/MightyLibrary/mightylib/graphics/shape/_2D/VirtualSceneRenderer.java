package MightyLibrary.mightylib.graphics.shape._2D;

import MightyLibrary.mightylib.main.WindowInfo;

public class VirtualSceneRenderer extends RectangleRenderer {
    private final FrameBuffer frameBuffer;

    public VirtualSceneRenderer(WindowInfo info){
        super(info,"postProcessing");
        frameBuffer = new FrameBuffer(info);
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