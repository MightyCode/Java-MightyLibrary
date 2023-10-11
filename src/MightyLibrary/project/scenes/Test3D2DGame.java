package MightyLibrary.project.scenes;

import MightyLibrary.mightylib.graphics.renderer._2D.Animation2DRenderer;
import MightyLibrary.mightylib.graphics.renderer._2D.FrameBuffer;
import MightyLibrary.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import MightyLibrary.mightylib.graphics.text.ETextAlignment;
import MightyLibrary.mightylib.graphics.text.Text;
import MightyLibrary.mightylib.main.GameTime;
import MightyLibrary.mightylib.resources.animation.AnimationData;
import MightyLibrary.mightylib.resources.animation.Animator;
import MightyLibrary.mightylib.resources.texture.BasicBindableObject;
import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.resources.texture.TextureParameters;
import MightyLibrary.mightylib.graphics.renderer._3D.shape.CubeRenderer;
import MightyLibrary.mightylib.scenes.Camera2D;
import MightyLibrary.mightylib.scenes.Camera3DCreationInfo;
import MightyLibrary.mightylib.scenes.Scene;
import MightyLibrary.mightylib.utils.math.ColorList;
import MightyLibrary.mightylib.utils.math.EDirection;
import MightyLibrary.project.main.ActionId;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;

public class Test3D2DGame extends Scene {
    private final static Camera3DCreationInfo SCENE_CCI = new Camera3DCreationInfo(120, new Vector3f(0, 4, 0));

    //// 2D World ////
    private Camera2D insideCamera;
    private RectangleRenderer background;
    private Animation2DRenderer slimeRenderer;
    private FrameBuffer game2DRender;

    // 3D World ////

    private RectangleRenderer debugView;
    private CubeRenderer gameRenderer;
    private Text lookAtText;

    public Test3D2DGame(){
        super(SCENE_CCI);
    }

    public void init(String[] args){
        super.init(args);
        /// SCENE INFORMATION ///

        mainContext.getMouseManager().setCursor(false);
        setClearColor(52, 189, 235, 1f);

        main3DCamera.setSpeed(new Vector3f(10));

        insideCamera = new Camera2D(mainContext.getWindow().getInfo(), new Vector2f(0, 0), true);

        /// RENDERERS ///
        game2DRender = new FrameBuffer(
                new BasicBindableObject().setQualityTexture(TextureParameters.PIXEL_ART_PARAMETERS),
                mainContext.getWindow().getInfo().getVirtualSizeRef().x,
                mainContext.getWindow().getInfo().getVirtualSizeRef().y);

        background = new RectangleRenderer("texture2D");
        background.setReferenceCamera(insideCamera);
        background.setMainTextureChannel("error");
        background.setSizePix(mainContext.getWindow().getInfo().getVirtualSizeRef().x,
                mainContext.getWindow().getInfo().getVirtualSizeRef().y);
        background.setPosition(new Vector2f(0, 0));

        slimeRenderer = new Animation2DRenderer("texture2D");
        slimeRenderer.setReferenceCamera(insideCamera);
        slimeRenderer.setMainTextureChannel("slime");
        Animator animator = new Animator();
        animator.addAndInitAnimation("first", resources.getResource(AnimationData.class, "slime"), true);
        slimeRenderer.init(animator);
        slimeRenderer.setShiftRotation(EDirection.None);

        float scale = 720.f / 30.f / 2;
        slimeRenderer.setScale(new Vector2f(scale));
        slimeRenderer.setPosition(new Vector2f(400, mainContext.getWindow().getInfo().getSizeCopy().y));
        slimeRenderer.setVerticalFlip(false);
        slimeRenderer.update();

        debugView = new RectangleRenderer("texture2D");
        debugView.setMainTextureChannel(game2DRender);
        debugView.setPosition(new Vector2f(0, 0));
        debugView.setSizePix(mainContext.getWindow().getInfo().getVirtualSizeRef().x * 0.2f,
                mainContext.getWindow().getInfo().getVirtualSizeRef().y * 0.2f);

        gameRenderer = new CubeRenderer("texture3D");
        gameRenderer.setMainTextureChannel(
                game2DRender
                /*Resources.getInstance().getResource(Texture.class, "error")*/);

        gameRenderer.setPosition(new Vector3f(-40f, 0.0f, 0f));
        gameRenderer.setScale(new Vector3f(10));

        gameRenderer.setTexturePosition();

        Vector2i windowSize = mainContext.getWindow().getInfo().getSizeRef();

        lookAtText = new Text();
        lookAtText.setFont("bahnschrift")
                .setAlignment(ETextAlignment.Center)
                .setReference(EDirection.Up)
                .setPosition(new Vector2f(windowSize.x * 0.5f, windowSize.y * 0.01f))
                .setFontSize(40)
                .setColor(ColorList.Coral())
                .setText("(0, 0, 0)");
    }


    public void update() {
        super.update();

        if (mainContext.getInputManager().inputPressed(ActionId.ESCAPE))
            sceneManagerInterface.setNewScene(new MenuScene(), new String[]{});

        InputManager inputManager = mainContext.getInputManager();

        int accelFactor = 1;
        if (inputManager.getState(ActionId.SHIFT))
            accelFactor = 3;

        if (inputManager.getState(ActionId.MOVE_LEFT))
            main3DCamera.speedAngX(main3DCamera.getSpeed().x * accelFactor * GameTime.DeltaTime());

        if (inputManager.getState(ActionId.MOVE_RIGHT))
            main3DCamera.speedAngX(-main3DCamera.getSpeed().x * accelFactor * GameTime.DeltaTime());

        if (inputManager.getState(ActionId.MOVE_FORWARD))
            main3DCamera.speedAngZ(-main3DCamera.getSpeed().z * accelFactor * GameTime.DeltaTime());

        if (inputManager.getState(ActionId.MOVE_BACKWARD))
            main3DCamera.speedAngZ(main3DCamera.getSpeed().z * accelFactor * GameTime.DeltaTime());

        if (inputManager.getState(ActionId.MOVE_UP))
            main3DCamera.setY(main3DCamera.getCamPosRef().y + main3DCamera.getSpeed().y * GameTime.DeltaTime());

        if (inputManager.getState(ActionId.MOVE_DOWN))
            main3DCamera.setY(main3DCamera.getCamPosRef().y - main3DCamera.getSpeed().y * GameTime.DeltaTime());

        float speed2D = 200;

        if (inputManager.getState(ActionId.MOVE_UP_2D)) {
            System.out.println(slimeRenderer.position().y);
            System.out.println(slimeRenderer.position().y - speed2D * GameTime.DeltaTime());
            slimeRenderer.setPosition(new Vector2f(
                    slimeRenderer.position().x,
                    slimeRenderer.position().y - speed2D * GameTime.DeltaTime()));
            System.out.println(slimeRenderer.position().y);
        }

        if (inputManager.getState(ActionId.MOVE_DOWN_2D))
            slimeRenderer.setPosition(new Vector2f(
                    slimeRenderer.position().x,
                    slimeRenderer.position().y + speed2D * GameTime.DeltaTime()));

        if (inputManager.getState(ActionId.MOVE_LEFT_2D))
            slimeRenderer.setPosition(new Vector2f(
                    slimeRenderer.position().x - speed2D * GameTime.DeltaTime(),
                    slimeRenderer.position().y));

        if (inputManager.getState(ActionId.MOVE_RIGHT_2D))
            slimeRenderer.setPosition(new Vector2f(
                    slimeRenderer.position().x + speed2D * GameTime.DeltaTime(),
                    slimeRenderer.position().y));


        if(inputManager.inputPressed(ActionId.TAB)) {
            main3DCamera.invertLockViewCursor();
            mainContext.getMouseManager().invertCursorState();
        }

        slimeRenderer.update();

        main3DCamera.updateView();

        lookAtText.setText("( " + String.format("%.2f", main3DCamera.getLookAtVector().x) + ", " +
                String.format("%.2f", main3DCamera.getLookAtVector().y) + ", " +
                String.format("%.2f", main3DCamera.getLookAtVector().z) + " )");
    }


    public void display() {
        game2DRender.bindFrameBuffer();
        clear();
        mainContext.getWindow().setVirtualViewport();

        background.display();
        slimeRenderer.display();

        game2DRender.unbindFrameBuffer();
        super.setVirtualScene();
        clear();

        // Better to draw the world here
        gameRenderer.display();

        // Better to draw the hud here and be not affected by the postProcessing shader

        lookAtText.display();

        debugView.display();

        super.setAndDisplayRealScene();
    }


    public void unload(){
        super.unload();
        gameRenderer.unload();
        game2DRender.unload();
        background.unload();
        slimeRenderer.unload();
        debugView.unload();

        lookAtText.unload();
    }
}