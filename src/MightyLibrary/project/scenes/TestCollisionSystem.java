package MightyLibrary.project.scenes;

import MightyLibrary.mightylib.graphics.shape._2D.TextureRenderer;
import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.main.GameTime;
import MightyLibrary.mightylib.physics.collision.CollisionVisualisation;
import MightyLibrary.mightylib.scene.Scene;
import MightyLibrary.mightylib.physics.collision.CollisionBoundedVolume2D;
import MightyLibrary.mightylib.physics.collision.CollisionRectangle;
import MightyLibrary.mightylib.util.math.Color4f;
import MightyLibrary.mightylib.util.math.EDirection;
import MightyLibrary.mightylib.util.math.MightyMath;
import MightyLibrary.mightylib.physics.tweenings.ETweeningBehaviour;
import MightyLibrary.mightylib.physics.tweenings.ETweeningOption;
import MightyLibrary.mightylib.physics.tweenings.ETweeningType;
import MightyLibrary.mightylib.physics.tweenings.type.FloatTweening;
import MightyLibrary.project.lib.ActionId;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class TestCollisionSystem extends Scene {
    private TextureRenderer renderer;
    private CollisionRectangle rectangle;
    private CollisionBoundedVolume2D boundedVolume2D;

    private TextureRenderer renderer2;
    private CollisionRectangle rectangle2;

    FloatTweening rotation;

    public void init(String[] args) {
        super.init(args);
        /// SCENE INFORMATION ///

        main3DCamera.setPos(new Vector3f(0, 0, 0));
        setClearColor(52, 189, 235, 1f);

        /// RENDERERS ///

        rectangle = new CollisionRectangle(0, 0, 500, 500);

        renderer = CollisionVisualisation.createFrom(rectangle, new Color4f(0.1f, 0.2f, 0.6f, 1f));

        boundedVolume2D = new CollisionBoundedVolume2D();
        boundedVolume2D.Collisions.add(rectangle);

        rectangle2 = new CollisionRectangle(600, 600, 200, 200);

        renderer2 = CollisionVisualisation.createFrom(rectangle2, new Color4f(0.7f, 0.6f, 0.6f, 1f)); new TextureRenderer("colorShape2D");

        rotation = new FloatTweening();

        rotation.setTweeningOption(ETweeningOption.LoopReversed)
                .setTweeningValues(ETweeningType.Sinusoidal, ETweeningBehaviour.InOut)
                .initTwoValue(2, 0f, MightyMath.PI_FLOAT * 2f);
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

        if (inputManager.input(ActionId.MOVE_UP)){
            main2DCamera.setY(main2DCamera.getCamPosRef().y - 100 * GameTime.DeltaTime());
            System.out.println("DOwn");
        }

        renderer.switchToColorMode(new Color4f(0.1f, 0.2f, 0.6f, 1f));

        if (boundedVolume2D.isColliding(rectangle2)){
            renderer.switchToColorMode(new Color4f(1, 0, 0, 1));
            boundedVolume2D.replaceComparedTo(rectangle2, EDirection.LeftUp);
            update = true;
        }

        if (update){
            renderer2.setPosition(new Vector2f(rectangle2.x(), rectangle2.y()));
            renderer.setPosition(new Vector2f(rectangle.x(), rectangle.y()));
        }


        rotation.update();
        //renderer.setRotation(rotation.value(), new Vector3f(0, 0, 1));

        renderer2.updateShape();

        main3DCamera.updateView();
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
