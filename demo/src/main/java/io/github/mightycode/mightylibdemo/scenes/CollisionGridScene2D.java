package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.graphics.text.ETextAlignment;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.physics.collision.Collision2D;
import io.github.mightycode.mightylib.physics.collision.Collision2DGrid;
import io.github.mightycode.mightylib.physics.collision.CollisionRectangle;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylibdemo.main.ActionId;
import io.github.mightycode.mightylibdemo.main.DemoCamera;
import org.joml.Vector2f;

import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;

/**
 * Collision2DGrid : the static obstacles are sorted into the cells of a grid once, then only the obstacles of the
 * cells touched by the moving rectangle (it follows the mouse) are tested against it. Gray : obstacles ignored by the
 * grid, blue : tested, red : tested and colliding.
 */
public class CollisionGridScene2D extends Scene {
    private static final float WIDTH = 1280f;
    private static final float HEIGHT = 720f;
    private static final float CELL = 80f;
    private static final int OBSTACLES = 150;

    private static final Color4f IGNORED = new Color4f(0.3f, 0.3f, 0.35f, 1f);
    private static final Color4f TESTED = new Color4f(0.2f, 0.4f, 0.9f, 1f);
    private static final Color4f COLLIDING = new Color4f(0.9f, 0.2f, 0.2f, 1f);
    private static final Color4f GRID_LINE = new Color4f(1f, 1f, 1f, 0.12f);

    private final List<CollisionRectangle> obstacles = new ArrayList<>();
    private final Set<Collision2D> tested = new HashSet<>();
    private final Set<Collision2D> colliding = new HashSet<>();

    private Collision2DGrid grid;
    private CollisionRectangle player;

    private RectangleRenderer rectangle;
    private Text info;

    @Override
    public void launch(String[] args) {
        setClearColor(0.1f, 0.1f, 0.12f);
        context().getMouseManager().setCursor(true);
        Camera2D hud = camera2D().copy();
        DemoCamera.free(this);

        // Fixed seed : the same obstacles at every launch
        Random random = new Random(42);
        Set<Collision2D> all = new HashSet<>();
        for (int i = 0; i < OBSTACLES; ++i) {
            CollisionRectangle obstacle = new CollisionRectangle(
                    random.nextFloat() * (WIDTH - 60), random.nextFloat() * (HEIGHT - 60),
                    15 + random.nextFloat() * 40, 15 + random.nextFloat() * 40);
            obstacles.add(obstacle);
            all.add(obstacle);
        }

        grid = new Collision2DGrid();
        grid.init(new Vector2f(WIDTH, HEIGHT), new Vector2f(CELL, CELL), all);

        player = new CollisionRectangle(0, 0, 70, 70);
        rectangle = new RectangleRenderer("colorShape2D");

        info = new Text();
        info.setFontSize(24)
                .setReference(EDirection.LeftUp)
                .setAlignment(ETextAlignment.Left)
                .setColor(new Color4f(1, 1, 1, 1))
                .setPosition(new Vector2f(20, 15));
        info.setReferenceCamera(hud);
    }

    @Override
    public void update() {
        super.update();

        if (context().getInputManager().inputPressed(ActionId.ESCAPE))
            sceneManager().setNewScene(new MenuScene(), new String[]{});

        Vector2f mouse = camera2D().transformPosition(context().getMouseManager().pos());
        player.setPosition(mouse.x - player.w() / 2f, mouse.y - player.h() / 2f);

        tested.clear();
        tested.addAll(grid.getCollisionNear(player));

        colliding.clear();
        for (Collision2D other : tested) {
            if (other.isColliding(player))
                colliding.add(other);
        }

        info.setText("Obstacles : " + OBSTACLES + "    tested : " + tested.size() + "    colliding : " + colliding.size());
    }

    private void draw(CollisionRectangle area, Color4f color) {
        rectangle.setColorMode(color);
        rectangle.setPosition(new Vector2f(area.x(), area.y()));
        rectangle.setSizePix(area.w(), area.h());
        rectangle.display();
    }

    @Override
    public void display() {
        setVirtualScene();
        clear();

        // The cells of the grid
        for (int column = 1; column * CELL < WIDTH; ++column)
            draw(new CollisionRectangle(column * CELL, 0, 1, HEIGHT), GRID_LINE);
        for (int row = 1; row * CELL < HEIGHT; ++row)
            draw(new CollisionRectangle(0, row * CELL, WIDTH, 1), GRID_LINE);

        for (CollisionRectangle obstacle : obstacles) {
            Color4f color = IGNORED;
            if (colliding.contains(obstacle))
                color = COLLIDING;
            else if (tested.contains(obstacle))
                color = TESTED;

            draw(obstacle, color);
        }

        draw(player, new Color4f(0.95f, 0.8f, 0.2f, 0.6f));

        info.display();

        setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();

        if (rectangle == null)
            return;

        rectangle.unload();
        info.unload();
    }
}
