package MightyLibrary.mightylib.physics.collision;

import MightyLibrary.mightylib.graphics.shape._2D.TextureRenderer;
import MightyLibrary.mightylib.util.math.Color4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

public abstract class CollisionVisualisation {
    public static TextureRenderer createFrom(CollisionRectangle rectangle, Color4f color){
        TextureRenderer renderer = new TextureRenderer("colorShape2D");
        renderer.setScale(new Vector3f(rectangle.w(), rectangle.h(), 1.f));

        renderer.setPosition(new Vector2f(rectangle.x(), rectangle.y()));
        renderer.switchToColorMode(color);

        return renderer;
    }
}
