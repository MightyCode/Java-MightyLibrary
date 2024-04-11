package MightyLibrary.mightylib.physics.collision;

import MightyLibrary.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import MightyLibrary.mightylib.utils.math.Color4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

public abstract class CollisionVisualisation {
    public static RectangleRenderer createFrom(CollisionRectangle rectangle, Color4f color){
        RectangleRenderer renderer = new RectangleRenderer("colorShape2D");
        renderer.setScale(new Vector3f(rectangle.w(), rectangle.h(), 1.f));

        renderer.setPosition(new Vector2f(rectangle.x(), rectangle.y()));
        renderer.setColorMode(color);

        return renderer;
    }
}
