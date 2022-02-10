package MightyLibrary.project.scenes;

import MightyLibrary.mightylib.graphics.shape._2D.Animation2DRenderer;;
import MightyLibrary.mightylib.graphics.text.ETextAlignment;
import MightyLibrary.mightylib.graphics.text.Text;
import MightyLibrary.mightylib.graphics.texture.AnimationData;
import MightyLibrary.mightylib.graphics.texture.Animator;
import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.scene.Camera3D;
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
    private Vector2fTweening slimeTextureTweening;

    private Text text;

    public void init(String[] args) {
        super.init(args);
        /// SCENE INFORMATION ///

        mainContext.getMouseManager().setCursor(false);
        main3DCamera.setPos(new Vector3f(0, 0, 0));
        setClearColor(52, 189, 235, 1f);

        /// RENDERERS ///

        slimeRenderer = new Animation2DRenderer("texture2D");
        slimeRenderer.switchToTextureMode("slime");

        Animator animator = new Animator();
        animator.addAndInitAnimation("first", resources.getResource(AnimationData.class, "slime"), true);

        slimeRenderer.init(animator);
        float scale = 720.f / 30.f;
        slimeRenderer.setScale(new Vector2f(scale));
        slimeRenderer.setPosition(new Vector2f(400, 400));

        slimeTextureTweening = new Vector2fTweening();

        slimeTextureTweening.setTweeningValues(ETweeningType.Back, ETweeningBehaviour.InOut)
                .initTwoValue(1, new Vector2f(400, 400), new Vector2f(400 + 150, 400 - 150))
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
            main3DCamera.speedAngX(Camera3D.speed.x * speed);
        }

        if (inputManager.input(ActionId.MOVE_RIGHT)) {
            main3DCamera.speedAngX(-Camera3D.speed.x * speed);
        }

        if (inputManager.input(ActionId.MOVE_FORWARD)) {
            main3DCamera.speedAngZ(-Camera3D.speed.z * speed);
        }

        if (inputManager.input(ActionId.MOVE_BACKWARD)) {
            main3DCamera.speedAngZ(Camera3D.speed.z * speed);
        }

        if (inputManager.input(ActionId.MOVE_UP)) {
            main3DCamera.setY(main3DCamera.getCamPosRef().y += Camera3D.speed.y);

            //rendererPosition.x += 150 * GameTime.DeltaTime();
        }

        if (inputManager.input(ActionId.MOVE_DOWN)) {
            main3DCamera.setY(main3DCamera.getCamPosRef().y -= Camera3D.speed.y);
        }

        if (inputManager.inputPressed(ActionId.ESCAPE)) {
            main3DCamera.invertLockViewCursor();
            mainContext.getMouseManager().invertCursorState();
        }

        main3DCamera.updateView();

        slimeTextureTweening.update();
        slimeRenderer.setPosition(slimeTextureTweening.value());

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
