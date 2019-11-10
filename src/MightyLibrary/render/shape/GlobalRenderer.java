package MightyLibrary.render.shape;

import MightyLibrary.util.EShapeType;

public class GlobalRenderer extends Renderer {
    private EShapeType type;

    public GlobalRenderer(String shaderName, boolean useEbo, boolean in2D) {
        super(shaderName, useEbo, in2D);
    }

    public void givesShape(EShapeType type){

    }

    public Shape getShape(){
        return shape;
    }

    public void setShape(Shape shape){
        this.shape = shape;
    }
}
