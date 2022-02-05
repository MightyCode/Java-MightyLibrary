package MightyLibrary.project.scenes;

import MightyLibrary.mightylib.graphics.shape._2D.Animation2DRenderer;;
import MightyLibrary.mightylib.graphics.text.ETextAlignment;
import MightyLibrary.mightylib.graphics.text.Text;
import MightyLibrary.mightylib.graphics.texture.AnimationData;
import MightyLibrary.mightylib.graphics.texture.Animator;
import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.main.GameTime;
import MightyLibrary.mightylib.scene.Camera;
import MightyLibrary.mightylib.scene.Scene;
import MightyLibrary.mightylib.util.math.EDirection;
import MightyLibrary.mightylib.util.tweenings.ETweeningBehaviour;
import MightyLibrary.mightylib.util.tweenings.ETweeningOption;
import MightyLibrary.mightylib.util.tweenings.ETweeningType;
import MightyLibrary.mightylib.util.tweenings.type.Vector2fTweening;
import MightyLibrary.project.lib.ActionId;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;

public class Test2DScene extends Scene {
    private Animation2DRenderer slimeRenderer;
    private Vector2f rendererPosition;
    private Vector2fTweening slimeTextureTweening;

    private Text text;

    public void init(String[] args) {
        super.init(args);
        /// SCENE INFORMATION ///

        mainContext.getMouseManager().setCursor(false);
        mainCamera.setPos(new Vector3f(0, 0, 0));
        setClearColor(52, 189, 235, 1f);

        /// RENDERERS ///

        slimeRenderer = new Animation2DRenderer(mainContext.getWindow().getInfo(), "texture2D");
        slimeRenderer.switchToTextureMode("slime");

        Animator animator = new Animator();
        animator.addAndInitAnimation("first", resources.getResource(AnimationData.class, "slime"), true);

        rendererPosition = new Vector2f();
        slimeRenderer.init(animator, rendererPosition);

        float scale = 720.f / 30.f;
        slimeRenderer.setScale(scale);
        rendererPosition.x = (14 * scale);
        rendererPosition.y = (22 * scale);

        slimeTextureTweening = new Vector2fTweening();

        slimeTextureTweening.setTweeningValues(ETweeningType.Quintic, ETweeningBehaviour.InOut)
                .initTwoValue(1, new Vector2f(0, 0), new Vector2f(150, -150))
                .setTweeningOption(ETweeningOption.LoopReversed).setAdditionnalArguments(3f,  null);


        text = new Text(mainContext.getWindow().getInfo());

        Vector2i size = mainContext.getWindow().getInfo().getSizeRef();

        text.setFont("arial")
                .setFontSize(60)
                .setReference(EDirection.RightDown)
                .setAlignment(ETextAlignment.Right)
                .setPosition(new Vector2f(size.x, size.y))
                .setText("Test d'écrire du text c'est super cool");
    }


    public void update() {
        super.update();

        InputManager inputManager = mainContext.getInputManager();

        int speed = 1;
        if (inputManager.inputPressed(ActionId.SHIFT)) {
            speed = 3;
        }

        if (inputManager.input(ActionId.MOVE_LEFT)) {
            mainCamera.speedAngX(Camera.speed.x * speed);
        }

        if (inputManager.input(ActionId.MOVE_RIGHT)) {
            mainCamera.speedAngX(-Camera.speed.x * speed);
        }

        if (inputManager.input(ActionId.MOVE_FORWARD)) {
            mainCamera.speedAngZ(-Camera.speed.z * speed);
        }

        if (inputManager.input(ActionId.MOVE_BACKWARD)) {
            mainCamera.speedAngZ(Camera.speed.z * speed);
        }

        if (inputManager.input(ActionId.MOVE_UP)) {
            mainCamera.setY(mainCamera.getCamPosRef().y += Camera.speed.y);

            rendererPosition.x += 150 * GameTime.DeltaTime();
        }

        if (inputManager.input(ActionId.MOVE_DOWN)) {
            mainCamera.setY(mainCamera.getCamPosRef().y -= Camera.speed.y);
        }

        if (inputManager.inputPressed(ActionId.ESCAPE)) {
            mainCamera.invertLockViewCursor();
            mainContext.getMouseManager().invertCursorState();
        }

        mainCamera.updateView();

        slimeTextureTweening.update();
        float scale = 720.f / 30.f;
        rendererPosition.x = (14 * scale) + slimeTextureTweening.value().x;
        rendererPosition.y = (22 * scale) + slimeTextureTweening.value().y;

        slimeRenderer.update();
    }


    public void display() {
        super.setVirtualScene();
        clear();

        slimeRenderer.display();

        text.display();

        super.setAndDisplayRealScene();
    }


    public void unload() {
        super.unload();
        slimeRenderer.unload();

        text.unload();
    }
}
