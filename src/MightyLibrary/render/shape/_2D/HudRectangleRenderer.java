package MightyLibrary.render.shape._2D;

import MightyLibrary.main.ManagerContainer;
import MightyLibrary.main.Window;
import MightyLibrary.render.shape.Renderer;
import MightyLibrary.render.shape.Shape;
import org.joml.Vector2f;

public class HudRectangleRenderer extends Renderer {
    protected Window window;
    float windowW, windowH, posX, posY;
    int positionIndex;


    public HudRectangleRenderer(String shaderName, float width, float height) {
        super(shaderName, true, true);
        window = ManagerContainer.getInstance().window;
        posX = 0;
        posY = 0;

        windowW = width * 2.0f / window.size.x - 1.0f;
        windowH = height * 2.0f / window.size.y - 1.0f;

        float[] positions = calculatePosition();
        int[] indices = { 0, 1, 2, 2, 1, 3 };
        shape.setEboStorage(Shape.STATIC_STORE);
        shape.setEbo(indices);
        positionIndex = shape.addVbo(positions, 2, Shape.STATIC_STORE);
    }


    private float[] calculatePosition(){
        return new float[]{
                -1.0f + posX, 1.0f - posY,
                -1.0f + posX, -windowH - posY,
                windowW + posX, 1.0f - posY,
                windowW + posX, -windowH - posY
        };
    }


    public void setPosition(Vector2f position){
        setPosition(position.x, position.y);
    }


    public void setPosition(float posX, float posY){
        this.posX = posX * 2.0f / window.size.x;
        this.posY = posY * 2.0f / window.size.y;;
        shape.resetVbo(calculatePosition(), positionIndex);
    }
}
