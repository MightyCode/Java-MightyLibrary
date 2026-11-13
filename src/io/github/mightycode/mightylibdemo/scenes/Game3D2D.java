package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.renderer._2D.Animation2DRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.FrameBuffer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.main.utils.GameTime;
import io.github.mightycode.mightylib.resources.animation.AnimationData;
import io.github.mightycode.mightylib.resources.animation.Animator;
import io.github.mightycode.mightylib.graphics.surface.BasicBindableObject;
import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.graphics.surface.TextureParameters;
import io.github.mightycode.mightylib.graphics.renderer._3D.shape.CubeRenderer;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.scenes.camera.Camera3DCreationInfo;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.camera.cameraComponents.DebugInfoCamera3D;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylib.scenes.camera.cameraComponents.FreeCamera3DComponent;
import io.github.mightycode.mightylibdemo.main.ActionId;
import io.github.mightycode.mightylibdemo.main.AxisId;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;

public class Game3D2D extends Scene {
    private FreeCamera3DComponent cameraComponent;

    private final static Camera3DCreationInfo SCENE_CCI = new Camera3DCreationInfo(120, new Vector3f(0, 4, 0));

    //// 2D World ////
    private Camera2D insideCamera;
    private RectangleRenderer background;
    private Animation2DRenderer slimeRenderer;
    private FrameBuffer game2DRender;

    // 3D World ////

    private RectangleRenderer debugView;
    private CubeRenderer gameRenderer;

    public Game3D2D(){
        super(SCENE_CCI);
    }

    @Override
    protected String[] getInvolvedBatch() {
        return new String[]{
                "anim"
        };
    }

    public void launch(String[] args){
        /// SCENE INFORMATION ///

        mainContext.getMouseManager().setCursor(false);
        cameraComponent = new FreeCamera3DComponent(mainContext.getInputManager(), main3DCamera, AxisId.freeCamera3DInputs());
        addUpdatable(cameraComponent);
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

        Vector2i windowSize = mainContext.getWindow().getInfo().getVirtualSizeRef();

        debugView.setSizePix(mainContext.getWindow().getInfo().getVirtualSizeRef().x * 0.2f,
                mainContext.getWindow().getInfo().getVirtualSizeRef().y * 0.2f);

        debugView.setPosition(new Vector2f(0, windowSize.y - debugView.scale().y));

        gameRenderer = new CubeRenderer("texture3D");
        gameRenderer.setMainTextureChannel(
                game2DRender
                );

        gameRenderer.setPosition(new Vector3f(-40f, 0.0f, 0f));
        gameRenderer.setScale(new Vector3f(10));

        gameRenderer.setTexturePosition();

        addUpdatableAndDisplayable(
                new DebugInfoCamera3D().init(main3DCamera, new Vector2f(5, 5))
                        .addInfo(DebugInfoCamera3D.POSITION)
                        .addInfo(DebugInfoCamera3D.LOOK)
                        .addInfo(DebugInfoCamera3D.SPEED)
        );
    }


    public void update() {
        super.update();

        if (mainContext.getInputManager().inputPressed(ActionId.ESCAPE))
            sceneManagerInterface.setNewScene(new MenuScene(), new String[]{});

        InputManager inputManager = mainContext.getInputManager();

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
            cameraComponent.toggleLookLocked();
            mainContext.getMouseManager().invertCursorState();
        }

        slimeRenderer.update();

        main3DCamera.updateView();
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

        debugView.display();

        super.display();

        super.setAndDisplayRealScene();
    }

    @Override
    public void unload(){
        super.unload();
        gameRenderer.unload();
        game2DRender.unload();
        background.unload();
        slimeRenderer.unload();
        debugView.unload();
    }
}