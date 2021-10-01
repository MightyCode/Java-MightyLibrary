package MightyLibrary.mightylib.graphics.shape._2D;

public class VirtualSceneRenderer extends RectangleRenderer {
    private FrameBuffer frameBuffer;

    public VirtualSceneRenderer(){
        super("postProcessing");
        frameBuffer = new FrameBuffer();
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