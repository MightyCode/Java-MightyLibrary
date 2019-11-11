package MightyLibrary.render.shape._2D;

import MightyLibrary.render.shape.Shape;

public class VirtualSceneRenderer extends FrameBuffer {
    private Shape screenShape;

    public VirtualSceneRenderer(){
        super();
        screenShape = new Shape("postProcessing", false, true);
        float realPos[] = new float[]{
                -1.0f,  1.0f,
                -1.0f, -1.0f,
                1.0f, -1.0f,

                -1.0f,  1.0f,
                1.0f, -1.0f,
                1.0f,  1.0f
        };

        float virtualPos[] = new float[]{
                0.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f,

                0.0f, 1.0f,
                1.0f, 0.0f,
                1.0f, 1.0f
        };

        screenShape.addVbo(realPos, 2, Shape.STATIC_STORE);
        screenShape.addVbo(virtualPos, 2, Shape.STATIC_STORE);
    }

    public void display(){
        screenShape.display();
    }

    public void unload(){
        super.unload();
        screenShape.unload();
    }
}