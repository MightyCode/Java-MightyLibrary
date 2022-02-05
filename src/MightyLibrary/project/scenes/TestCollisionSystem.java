package MightyLibrary.project.scenes;

import MightyLibrary.mightylib.graphics.shape._2D.TextureRenderer;
import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.main.GameTime;
import MightyLibrary.mightylib.scene.Scene;
import MightyLibrary.mightylib.util.Timer;
import MightyLibrary.mightylib.util.collision.CollisionRectangle;
import MightyLibrary.mightylib.util.math.Color4f;
import MightyLibrary.project.lib.ActionId;
import org.joml.Vector3f;

public class TestCollisionSystem extends Scene {
    private TextureRenderer renderer;
    private CollisionRectangle rectangle;

    private TextureRenderer renderer2;
    private CollisionRectangle rectangle2;

    public void init(String[] args) {
        super.init(args);
        /// SCENE INFORMATION ///

        mainCamera.setPos(new Vector3f(0, 0, 0));
        setClearColor(52, 189, 235, 1f);

        /// RENDERERS ///

        renderer = new TextureRenderer(mainContext.getWindow().getInfo(), "colorShape2D");
        renderer.switchToColorMode(new Color4f(0.1f, 0.2f, 0.6f, 1f));
        renderer.setPosition(0, 0);
        renderer.setSizePix(500, 500);

        rectangle = new CollisionRectangle(0, 0, 500, 500);

        renderer2 = new TextureRenderer(mainContext.getWindow().getInfo(), "colorShape2D");
        renderer2.switchToColorMode(new Color4f(0.7f, 0.6f, 0.6f, 1f));
        renderer2.setSizePix(200, 200);

        rectangle2 = new CollisionRectangle(600, 600, 200, 200);
        renderer2.setPosition(rectangle2.x(), rectangle2.y());
    }


    public void update() {
        super.update();

        InputManager inputManager = mainContext.getInputManager();
        boolean update = false;

        if (inputManager.input(ActionId.MOVE_BACKWARD) && !inputManager.input(ActionId.MOVE_FORWARD)){
            update = true;

            rectangle2.moveY(500 * GameTime.DeltaTime());
        }

        if (inputManager.input(ActionId.MOVE_FORWARD) && !inputManager.input(ActionId.MOVE_BACKWARD)){
            update = true;

            rectangle2.moveY(-500 * GameTime.DeltaTime());
        }

        if (inputManager.input(ActionId.MOVE_LEFT) && !inputManager.input(ActionId.MOVE_RIGHT)){
            update = true;

            rectangle2.moveX(-500 * GameTime.DeltaTime());
        }

        if (inputManager.input(ActionId.MOVE_RIGHT) && !inputManager.input(ActionId.MOVE_LEFT)){
            update = true;

            rectangle2.moveX(500 * GameTime.DeltaTime());
        }

        if (update){
            renderer2.setPosition(rectangle2.x(), rectangle2.y());
        }

        if (rectangle2.isColliding(rectangle)){
            renderer.switchToColorMode(new Color4f(1, 0, 0, 1));
        } else {
            renderer.switchToColorMode(new Color4f(0.1f, 0.2f, 0.6f, 1f));
        }

        mainCamera.updateView();
    }


    public void display() {
        super.setVirtualScene();
        clear();

        renderer.display();
        renderer2.display();

        super.setAndDisplayRealScene();
    }


    public void unload() {
        super.unload();

        renderer.unload();
        renderer2.unload();
    }
}
