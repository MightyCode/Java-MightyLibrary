package MightyLibrary.project.scenes.Platformer2D;

import MightyLibrary.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import MightyLibrary.mightylib.physics.B2Utils;
import MightyLibrary.mightylib.utils.math.color.ColorList;
import MightyLibrary.mightylib.utils.math.geometry.EDirection;
import org.jbox2d.common.Vec2;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class PlayerRenderer {
    private final RectangleRenderer renderer;
    private final PlayerValues values;

    public PlayerRenderer(PlayerValues values) {
        this.values = values;

        renderer = new RectangleRenderer("colorShape2D");
        renderer.setColorMode(ColorList.Orange()); // Character color
        renderer.setReferenceDirection(EDirection.None);
        renderer.setSizePix(B2Utils.b2ToEngine(new Vec2(values.size.x, values.size.y)));
    }

    /** Update visual state from PlayerValues (position, rotation, etc.) */
    public void update() {
        renderer.setPosition(new Vector2f(B2Utils.b2ToEngine(values.position)));
        renderer.setRotation(0, new Vector3f(0, 0, 1));
    }

    /** Display the player */
    public void display() {
        renderer.display();
    }

    /** Free rendering resources */
    public void unload() {
        renderer.unload();
    }
}
