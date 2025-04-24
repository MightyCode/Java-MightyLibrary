package MightyLibrary.project.scenes;

import MightyLibrary.mightylib.graphics.GUI.BackgroundlessButton;
import MightyLibrary.mightylib.graphics.GUI.GUIList;
import MightyLibrary.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import MightyLibrary.mightylib.graphics.text.ETextAlignment;
import MightyLibrary.mightylib.graphics.surface.BasicBindableObject;
import MightyLibrary.mightylib.graphics.surface.TextureParameters;
import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.scenes.Scene;
import MightyLibrary.mightylib.sounds.SoundManager;
import MightyLibrary.mightylib.sounds.SoundSourceCreationInfo;
import MightyLibrary.mightylib.utils.math.color.Color4f;
import MightyLibrary.mightylib.utils.math.geometry.EDirection;
import MightyLibrary.mightylib.utils.math.MightyMath;
import MightyLibrary.mightylib.physics.tweenings.ETweeningBehaviour;
import MightyLibrary.mightylib.physics.tweenings.ETweeningOption;
import MightyLibrary.mightylib.physics.tweenings.ETweeningType;
import MightyLibrary.mightylib.physics.tweenings.type.FloatTweening;
import MightyLibrary.project.main.ActionId;
import MightyLibrary.project.scenes.loadingScenes.FirstLoadingScene;
import MightyLibrary.project.scenes.loadingScenes.LoadingSceneImplementation;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

public class MenuScene extends Scene {
    private static final SoundSourceCreationInfo CREATION_INFO_SELECT = new SoundSourceCreationInfo();
    static {
        CREATION_INFO_SELECT.name = "select";
        CREATION_INFO_SELECT.gainNode = "noise";
        CREATION_INFO_SELECT.gain = 1f;
    }

    private GUIList guiList;

    private BackgroundlessButton buttonQuit;

    private FloatTweening rotation;

    private RectangleRenderer test;

    public void init(String[] args) {
        if (args.length > 0 && args[0].equals("start")) {
            super.init(args, new FirstLoadingScene());
        } else {
            super.init(args, new LoadingSceneImplementation());
        }
    }

    public void launch(String[] args) {
        super.launch(args, new BasicBindableObject().setQualityTexture(TextureParameters.REALISTIC_PARAMETERS));
        /// SCENE INFORMATION ///

        mainContext.getMouseManager().setCursor(true);

        main3DCamera.setPos(new Vector3f(0, 0, 0));
        setClearColor(52, 189, 235, 1f);

        /// RENDERERS ///

        Vector2i windowSize = mainContext.getWindow().getInfo().getVirtualSizeCopy();

        BackgroundlessButton button2DScene = new BackgroundlessButton(mainContext);
        button2DScene.Text.setFont("bahnschrift")
                .setAlignment(ETextAlignment.Center)
                .setReference(EDirection.None)
                .setPosition(new Vector2f(windowSize.x * 0.25f, windowSize.y * 0.4f))
                .setFontSize(40)
                .setText("Test2DScene");

        button2DScene.Text.copyTo(button2DScene.OverlapsText);
        button2DScene.OverlapsText.setColor(new Color4f(0.3f))
                .setText("->Test2DScene<-");

        BackgroundlessButton Test2DWaveFunctionCollapse = button2DScene.copy();
        Test2DWaveFunctionCollapse.Text.setPosition(new Vector2f(windowSize.x * 0.75f, windowSize.y * 0.4f))
                .setText("Test2DWaveFunctionCollapse");

        Test2DWaveFunctionCollapse.Text.copyTo(Test2DWaveFunctionCollapse.OverlapsText);
        Test2DWaveFunctionCollapse.OverlapsText.setColor(new Color4f(0.3f))
                .setText("->Test2DWaveFunctionCollapse<-");

        BackgroundlessButton button3DScene = button2DScene.copy();
        button3DScene.Text.setPosition(new Vector2f(windowSize.x * 0.25f, windowSize.y * 0.52f))
                        .setText("Test3DScene");

        button3DScene.Text.copyTo(button3DScene.OverlapsText);
        button3DScene.OverlapsText.setColor(new Color4f(0.3f))
                .setText("->Test3DScene<-");

        BackgroundlessButton button3D_2Scene = button2DScene.copy();
        button3D_2Scene.Text.setPosition(new Vector2f(windowSize.x * 0.75f, windowSize.y * 0.52f))
                .setText("Test3DScene-2");

        button3D_2Scene.Text.copyTo(button3D_2Scene.OverlapsText);
        button3D_2Scene.OverlapsText.setColor(new Color4f(0.3f))
                .setText("->Test3DScene-2<-");


        BackgroundlessButton buttonCollisionTest = button2DScene.copy();
        buttonCollisionTest.Text.setPosition(new Vector2f(windowSize.x * 0.5f, windowSize.y * 0.64f))
                .setText("TestCollisionScene");

        buttonCollisionTest.Text.copyTo(buttonCollisionTest.OverlapsText);
        buttonCollisionTest.OverlapsText.setColor(new Color4f(0.3f))
                .setText("->TestCollisionScene<-");


        BackgroundlessButton button3D2DGame = button2DScene.copy();
        button3D2DGame.Text.setPosition(new Vector2f(windowSize.x * 0.25f, windowSize.y * 0.8f))
                .setText("Test 3D 2D game");

        button3D2DGame.Text.copyTo(button3D2DGame.OverlapsText);
        button3D2DGame.OverlapsText.setColor(new Color4f(0.3f))
                .setText("->Test 3D 2D game<-");

        BackgroundlessButton buttonTestBox2D = button2DScene.copy();
        buttonTestBox2D.Text.setPosition(new Vector2f(windowSize.x * 0.75f, windowSize.y * 0.8f))
                .setText("Test box 2D");

        buttonTestBox2D.Text.copyTo(buttonTestBox2D.OverlapsText);
        buttonTestBox2D.OverlapsText.setColor(new Color4f(0.3f))
                .setText("->Test box 2D<-");

        buttonQuit = button2DScene.copy();
        buttonQuit.Text.setPosition(new Vector2f(windowSize.x * 0.5f, windowSize.y * 0.9f))
                .setText("Quit");

        buttonQuit.Text.copyTo(buttonQuit.OverlapsText);
        buttonQuit.OverlapsText.setColor(new Color4f(0.3f))
                .setText("->Quit<-");

        guiList = new GUIList(mainContext.getInputManager(), mainContext.getMouseManager());
        guiList.setupActionInputValues(ActionId.SELECT_UP, ActionId.SELECT_DOWN);
        guiList.GUIs.put(0, button2DScene);
        guiList.GUIs.put(1, Test2DWaveFunctionCollapse);
        guiList.GUIs.put(2, button3DScene);
        guiList.GUIs.put(3, button3D_2Scene);
        guiList.GUIs.put(4, buttonCollisionTest);
        guiList.GUIs.put(5, button3D2DGame);
        guiList.GUIs.put(6, buttonTestBox2D);
        guiList.GUIs.put(7, buttonQuit);
        guiList.ShouldLoop = false;

        rotation = new FloatTweening();
        rotation.setTweeningOption(ETweeningOption.LoopReversed)
                .setTweeningValues(ETweeningType.Sinusoidal, ETweeningBehaviour.InOut)
                .initTwoValue(2, 0f, MightyMath.PI_FLOAT * 2f);

        test = new RectangleRenderer("texture2D");
        test.load(0);
        test.setMainTextureChannel("error");
        test.setSizePix(100, 100);
        test.setPosition(new Vector2f(500, 10));
    }

    @Override
    public void update() {
        super.update();

        guiList.update();

        if (mainContext.getInputManager().inputPressed(ActionId.ENTER)
                || mainContext.getInputManager().inputPressed(ActionId.LEFT_CLICK)) {
            Integer id = guiList.getSelected();

            if (id != null) {
                switch (id) {
                    case 0:
                        sceneManagerInterface.setNewScene(new Test2DScene(), new String[]{""});
                        break;
                    case 1:
                        sceneManagerInterface.setNewScene(new Test2DWaveFunctionCollapseScene(), new String[]{""});
                        break;
                    case 2:
                        sceneManagerInterface.setNewScene(new Test3DScene(), new String[]{""});
                        break;
                    case 3:
                        sceneManagerInterface.setNewScene(new Test3DScene2(), new String[]{""});
                        break;
                    case 4:
                        sceneManagerInterface.setNewScene(new TestCollisionSystem(), new String[]{""});
                        break;
                    case 5:
                        sceneManagerInterface.setNewScene(new Test3D2DGame(), new String[]{""});
                        break;
                    case 6:
                        sceneManagerInterface.setNewScene(new TestBox2D(), new String[]{""});
                        break;
                    case 7:
                        sceneManagerInterface.exit(0);
                        break;
                }
            }
        }

        rotation.update();
        buttonQuit.Text.setRotation(rotation.value(), new Vector3f(0, 0, 1));

        if (guiList.isStateChanged()){
            SoundManager.getInstance().createSoundSource(CREATION_INFO_SELECT);
        }

        //main2DCamera.setX(rotation.value() * 50);

        InputManager inputManager = mainContext.getInputManager();

        if (inputManager.inputPressed(ActionId.ESCAPE))
            sceneManagerInterface.exit(0);

        if (inputManager.getKeyboardManager().getKeyState(GLFW_KEY_LEFT_SHIFT) &&
                inputManager.getKeyboardManager().getKeyState(GLFW_KEY_F2) )
            sceneManagerInterface.setNewScene(new Test2DRopeSimulation(), new String[]{""});

    }

    @Override
    public void display() {
        super.setVirtualScene();
        clear();
        super.display();

        test.display();
        guiList.display();
        test.display();

        super.setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();

        if (guiList != null)
            guiList.unload();
    }
}
