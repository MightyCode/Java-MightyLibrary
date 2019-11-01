package MightyLibrary.render.shape._2D;

import MightyLibrary.render.shape.Shape;

public class VirtualSceneRenderer extends FrameBuffer {
    private Shape screenShape;

    public VirtualSceneRenderer(){
        super();
        screenShape = new Shape("postProcessing", false, true);
        float vertex2[] = new float[]{
                -1.0f,  1.0f,  0.0f, 1.0f,
                -1.0f, -1.0f,  0.0f, 0.0f,
                1.0f, -1.0f,  1.0f, 0.0f,

                -1.0f,  1.0f,  0.0f, 1.0f,
                1.0f, -1.0f,  1.0f, 0.0f,
                1.0f,  1.0f,  1.0f, 1.0f
        };

        screenShape.setReading(new int[]{2, 2});
        screenShape.setVbo(vertex2);
    }

    public void display(){
        screenShape.display();
    }

    public void unload(){
        super.unload();
        screenShape.unload();
    }
}
