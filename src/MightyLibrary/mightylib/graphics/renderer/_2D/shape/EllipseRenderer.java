package MightyLibrary.mightylib.graphics.renderer._2D.shape;

import MightyLibrary.mightylib.graphics.renderer.Renderer;
import MightyLibrary.mightylib.graphics.renderer.RendererUtils;
import MightyLibrary.mightylib.graphics.renderer.Shape;
import MightyLibrary.mightylib.util.math.EDirection;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class EllipseRenderer extends Renderer {
    protected EDirection reference;
    protected final int positionIndex;

    public EllipseRenderer(String shaderName) {
        super(shaderName, true, true);

        reference = EDirection.None;

        int[] indices = { 0, 1, 2, 2, 0, 3 };
        shape.setEboStorage(Shape.STATIC_STORE);
        shape.setEbo(indices);
        positionIndex = shape.addVbo(calculatePosition(), 2, Shape.STATIC_STORE);
    }


    private float[] calculatePosition(){
        return RendererUtils.calculatePositionForSquare(new Vector2f(1, 1), this.reference);
    }

    public EllipseRenderer setReference(EDirection reference){
        this.reference = reference;

        shape.updateVbo(calculatePosition(), positionIndex);

        return this;
    }

    // Set size with size of pixel
    public EllipseRenderer setSizePix(float width, float height){
        setScale(new Vector3f(width, height, 1.0f));

        return this;
    }

    public EllipseRenderer setPosition(Vector2f position){
        super.setPosition(new Vector3f(position.x, position.y, 0.0f));

        return this;
    }
}
