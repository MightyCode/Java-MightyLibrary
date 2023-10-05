package MightyLibrary.project.scenes;

import MightyLibrary.mightylib.graphics.renderer._2D.Animation2DRenderer;
import MightyLibrary.mightylib.graphics.renderer._2D.FrameBuffer;
import MightyLibrary.mightylib.graphics.shader.ShaderManager;
import MightyLibrary.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import MightyLibrary.mightylib.main.Window;
import MightyLibrary.mightylib.resources.animation.AnimationData;
import MightyLibrary.mightylib.resources.animation.Animator;
import MightyLibrary.mightylib.resources.texture.BasicBindableObject;
import MightyLibrary.mightylib.resources.texture.Texture;
import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.graphics.renderer.Renderer;
import MightyLibrary.mightylib.graphics.renderer._3D.ModelRenderer;
import MightyLibrary.mightylib.physics.tweenings.ETweeningBehaviour;
import MightyLibrary.mightylib.physics.tweenings.ETweeningOption;
import MightyLibrary.mightylib.physics.tweenings.ETweeningType;
import MightyLibrary.mightylib.physics.tweenings.type.FloatTweening;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.resources.texture.TextureParameters;
import MightyLibrary.mightylib.scene.Camera3D;
import MightyLibrary.mightylib.graphics.renderer._3D.shape.CubeRenderer;
import MightyLibrary.mightylib.graphics.renderer.Shape;
import MightyLibrary.mightylib.scene.Camera3DCreationInfo;
import MightyLibrary.mightylib.scene.Scene;
import MightyLibrary.mightylib.util.math.Color4f;
import MightyLibrary.mightylib.util.math.EDirection;
import MightyLibrary.project.lib.ActionId;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;

public class Test3D2DGame extends Scene {

    private final static Camera3DCreationInfo SCENE_CCI = new Camera3DCreationInfo(120, new Vector3f(0, 4, 0));

    //// 2D World ////

    private RectangleRenderer hudBar;
    private RectangleRenderer renderToWholeScreen;
    private FrameBuffer game2DRender;
    private Animation2DRenderer slimeRenderer;

    // 3D World ////

    private CubeRenderer gameRenderer;

    public Test3D2DGame(){
        super(SCENE_CCI);
    }

    public void init(String[] args){
        super.init(args);
        /// SCENE INFORMATION ///

        mainContext.getMouseManager().setCursor(false);
        setClearColor(52, 189, 235, 1f);

        /// RENDERERS ///
        game2DRender = new FrameBuffer(new BasicBindableObject().setQualityTexture(TextureParameters.PIXEL_ART_PARAMETERS),
                mainContext.getWindow().getInfo().getVirtualSizeRef().x,  mainContext.getWindow().getInfo().getVirtualSizeRef().y);

        hudBar = new RectangleRenderer("texture2D");
        hudBar.switchToTextureMode("error");
        hudBar.setSizePix(mainContext.getWindow().getInfo().getVirtualSizeRef().x, mainContext.getWindow().getInfo().getVirtualSizeRef().y);
        hudBar.setPosition(new Vector2f(0, 0));


        slimeRenderer = new Animation2DRenderer("texture2D");
        slimeRenderer.switchToTextureMode("slime");
        Animator animator = new Animator();
        animator.addAndInitAnimation("first", resources.getResource(AnimationData.class, "slime"), true);
        slimeRenderer.init(animator);
        slimeRenderer.setShiftRotation(EDirection.None);

        float scale = 720.f / 30.f / 2;
        slimeRenderer.setScale(new Vector2f(scale));
        slimeRenderer.setPosition(new Vector2f(400, mainContext.getWindow().getInfo().getSizeCopy().y));
        slimeRenderer.setVerticalFlip(false);
        slimeRenderer.update();

        renderToWholeScreen = new RectangleRenderer("texture2D");
        renderToWholeScreen.switchToTextureMode(game2DRender);
        renderToWholeScreen.setPosition(new Vector2f(0, 0));
        renderToWholeScreen.setSizePix(mainContext.getWindow().getInfo().getVirtualSizeRef().x * 0.2f,
                mainContext.getWindow().getInfo().getVirtualSizeRef().y * 0.2f);

        gameRenderer = new CubeRenderer("texture3D", new Vector3f(-1f, 3.0f, -4f), 10f);
        gameRenderer.switchToTextureMode(
                game2DRender
                /*Resources.getInstance().getResource(Texture.class, "error")*/);

        gameRenderer.setTexturePosition();
    }


    public void update() {
        super.update();

        InputManager inputManager = mainContext.getInputManager();

        int speed = 1;
        if (inputManager.getState(ActionId.SHIFT)) {
            speed = 3;
        }

        if(inputManager.getState(ActionId.MOVE_LEFT)){
            main3DCamera.speedAngX(Camera3D.speed.x * speed);
        }

        if(inputManager.getState(ActionId.MOVE_RIGHT)){
            main3DCamera.speedAngX(-Camera3D.speed.x * speed);
        }

        if(inputManager.getState(ActionId.MOVE_FORWARD)){
            main3DCamera.speedAngZ(-Camera3D.speed.z * speed);
        }

        if(inputManager.getState(ActionId.MOVE_BACKWARD)) {
            main3DCamera.speedAngZ(Camera3D.speed.z * speed);
        }

        if(inputManager.getState(ActionId.MOVE_UP)) {
            main3DCamera.setY(main3DCamera.getCamPosRef().y += Camera3D.speed.y);
        }

        if(inputManager.getState(ActionId.MOVE_DOWN)) {
            main3DCamera.setY(main3DCamera.getCamPosRef().y -= Camera3D.speed.y);
        }

        if(inputManager.inputPressed(ActionId.ESCAPE)) {
            main3DCamera.invertLockViewCursor();
            mainContext.getMouseManager().invertCursorState();
        }

        slimeRenderer.update();

        main3DCamera.updateView();
    }


    public void display() {
        game2DRender.bindFrameBuffer();
        clear();

        mainContext.getWindow().setVirtualViewport();

        hudBar.display();
        slimeRenderer.display();

        game2DRender.unbindFrameBuffer();
        super.setVirtualScene();
        clear();

        // Better to draw the world here
        gameRenderer.display();

        // Better to draw the hud here and be not affected by the postProcessing shader

        renderToWholeScreen.display();

        super.setAndDisplayRealScene();
    }


    public void unload(){
        super.unload();
        gameRenderer.unload();
        game2DRender.unload();
        hudBar.unload();
        slimeRenderer.unload();
        renderToWholeScreen.unload();
    }
}