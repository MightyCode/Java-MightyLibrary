package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.GUI.BackgroundlessButton;
import io.github.mightycode.mightylib.graphics.GUI.GUIList;
import io.github.mightycode.mightylib.graphics.GUI.ScrollList;
import io.github.mightycode.mightylib.graphics.text.ETextAlignment;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.scenes.DefaultLoadingScene;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.TemplateSceneLoading;
import io.github.mightycode.mightylib.sounds.SoundManager;
import io.github.mightycode.mightylib.sounds.SoundSourceCreationInfo;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylib.utils.math.MightyMath;
import io.github.mightycode.mightylib.physics.tweenings.ETweeningBehaviour;
import io.github.mightycode.mightylib.physics.tweenings.ETweeningOption;
import io.github.mightycode.mightylib.physics.tweenings.ETweeningType;
import io.github.mightycode.mightylib.physics.tweenings.type.FloatTweening;
import io.github.mightycode.mightylibdemo.main.ActionId;
import io.github.mightycode.mightylibdemo.scenes.Platformer2D.Test2DPlatformerBox2D;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.lwjgl.glfw.GLFW.*;

public class MenuScene extends Scene {
    private static final SoundSourceCreationInfo CREATION_INFO_SELECT = new SoundSourceCreationInfo();
    static {
        CREATION_INFO_SELECT.name = "selectogg";
        CREATION_INFO_SELECT.gainNode = "noise";
        CREATION_INFO_SELECT.gain = 0.2f;
    }

    private GUIList guiList;

    private ScrollList sceneList;
    private List<SceneEntry> sceneEntries;

    private BackgroundlessButton buttonQuit;

    private FloatTweening rotation;

    private Text title;

    @Override
    protected TemplateSceneLoading createLoadingScene(String[] args) {
        // Plain grey screen at the start of the program
        if (args.length > 0 && args[0].equals("start"))
            return new DefaultLoadingScene();

        return super.createLoadingScene(args);
    }

    public void launch(String[] args) {
        /// SCENE INFORMATION ///

        mainContext.getMouseManager().setCursor(true);

        main3DCamera.setPos(new Vector3f(0, 0, 0));
        setClearColor(52, 189, 235, 1f);

        /// RENDERERS ///

        Vector2i windowSize = mainContext.getWindow().getInfo().getVirtualSizeCopy();

        // Left : shortcuts to the main test scenes
        BackgroundlessButton button2DScene = createButton("Test2DScene", windowSize.x * 0.25f, windowSize.y * 0.4f, null);
        BackgroundlessButton button3DScene = createButton("Test3DScene", windowSize.x * 0.25f, windowSize.y * 0.52f, button2DScene);
        BackgroundlessButton button3D_2Scene = createButton("Test3DScene-2", windowSize.x * 0.25f, windowSize.y * 0.64f, button2DScene);
        buttonQuit = createButton("Quit", windowSize.x * 0.5f, windowSize.y * 0.9f, button2DScene);

        guiList = new GUIList(mainContext.getInputManager(), mainContext.getMouseManager());
        guiList.setupActionInputValues(ActionId.SELECT_UP, ActionId.SELECT_DOWN);
        guiList.GUIs.put(0, button2DScene);
        guiList.GUIs.put(1, button3DScene);
        guiList.GUIs.put(2, button3D_2Scene);
        guiList.GUIs.put(3, buttonQuit);
        guiList.setupActivationActions(ActionId.ENTER, ActionId.LEFT_CLICK);
        guiList.ShouldLoop = false;

        // Right : every scene of the demo, scrollable
        sceneEntries = createSceneEntries();
        List<String> names = new ArrayList<>();
        for (SceneEntry entry : sceneEntries)
            names.add(entry.name);

        sceneList = new ScrollList(mainContext, windowSize.x * 0.55f, windowSize.y * 0.25f,
                windowSize.x * 0.4f, windowSize.y * 0.56f, windowSize.y * 0.08f, "bahnschrift", 30);
        sceneList.setupActivationActions(ActionId.LEFT_CLICK);
        sceneList.setItems(names);

        rotation = new FloatTweening();
        rotation.setTweeningOption(ETweeningOption.LoopReversed)
                .setTweeningValues(ETweeningType.Sinusoidal, ETweeningBehaviour.InOut)
                .initTwoValue(2, 0f, MightyMath.PI_FLOAT * 2f);

        title = new Text();
        title.setFont("bahnschrift")
                .setAlignment(ETextAlignment.Center)
                .setReference(EDirection.None)
                .setFontSize(80)
                .setPosition(new Vector2f(windowSize.x * 0.5f, windowSize.y * 0.12f))
                .setText("MightyLibrary");
    }

    private static class SceneEntry {
        final String name;
        final Supplier<Scene> factory;

        SceneEntry(String name, Supplier<Scene> factory) {
            this.name = name;
            this.factory = factory;
        }
    }

    // Every scene reachable from the menu (add new test scenes here)
    private static List<SceneEntry> createSceneEntries() {
        List<SceneEntry> list = new ArrayList<>();
        list.add(new SceneEntry("Test2DScene", Test2DScene::new));
        list.add(new SceneEntry("Test2DWaveFunctionCollapse", Test2DWaveFunctionCollapseScene::new));
        list.add(new SceneEntry("Test2DRopeSimulation", Test2DRopeSimulation::new));
        list.add(new SceneEntry("Test2DPlatformerBox2D", Test2DPlatformerBox2D::new));
        list.add(new SceneEntry("TestBox2D", TestBox2D::new));
        list.add(new SceneEntry("TestCollisionSystem", TestCollisionSystem::new));
        list.add(new SceneEntry("Test3DScene", Test3DScene::new));
        list.add(new SceneEntry("Test3DScene-2", Test3DScene2::new));
        list.add(new SceneEntry("Test3DSceneTerrainGeneration", Test3DSceneTerrainGeneration::new));
        list.add(new SceneEntry("Test3D2DGame", Test3D2DGame::new));
        list.add(new SceneEntry("TestSoundFormatsScene", TestSoundFormatsScene::new));
        list.add(new SceneEntry("TestGamepadScene", TestGamepadScene::new));
        list.add(new SceneEntry("TestPreloadScene", TestPreloadScene::new));
        list.add(new SceneEntry("TestGLLoading", TestGLLoading::new));
        list.add(new SceneEntry("TestFallbackScene", TestFallbackScene::new));
        list.add(new SceneEntry("DocumentationScene", DocumentationScene::new));
        return list;
    }

    private BackgroundlessButton createButton(String label, float x, float y, BackgroundlessButton model) {
        BackgroundlessButton button;
        if (model == null) {
            button = new BackgroundlessButton(mainContext);
            button.Text.setFont("bahnschrift")
                    .setAlignment(ETextAlignment.Center)
                    .setReference(EDirection.None)
                    .setFontSize(40);
        } else {
            button = model.copy();
        }

        button.Text.setPosition(new Vector2f(x, y)).setText(label);
        button.Text.copyTo(button.OverlapsText);
        button.OverlapsText.setColor(new Color4f(0.3f)).setText("->" + label + "<-");

        return button;
    }

    @Override
    public void update() {
        super.update();

        guiList.update();

        sceneList.update();

        Integer activatedScene = sceneList.getActivated();
        Integer id = guiList.getActivated();

        if (activatedScene != null) {
            sceneManagerInterface.setNewScene(sceneEntries.get(activatedScene).factory.get(), new String[]{""});
        } else if (id != null) {
            switch (id) {
                case 0:
                    sceneManagerInterface.setNewScene(new Test2DScene(), new String[]{""});
                    break;
                case 1:
                    sceneManagerInterface.setNewScene(new Test3DScene(), new String[]{""});
                    break;
                case 2:
                    sceneManagerInterface.setNewScene(new Test3DScene2(), new String[]{""});
                    break;
                case 3:
                    sceneManagerInterface.exit(0);
                    break;
            }
        }

        rotation.update();
        buttonQuit.Text.setRotation(rotation.value(), new Vector3f(0, 0, 1));

        if (guiList.isStateChanged()){
            engine().soundManager().createSoundSource(CREATION_INFO_SELECT);
        }

        //main2DCamera.setX(rotation.value() * 50);

        InputManager inputManager = mainContext.getInputManager();

        if (inputManager.inputPressed(ActionId.ESCAPE))
            sceneManagerInterface.exit(0);

        if (inputManager.getKeyboardManager().getKeyState(GLFW_KEY_LEFT_SHIFT) &&
                inputManager.getKeyboardManager().getKeyState(GLFW_KEY_F2) )
            sceneManagerInterface.setNewScene(new Test2DRopeSimulation(), new String[]{""});

        if (inputManager.getKeyboardManager().getKeyState(GLFW_KEY_LEFT_SHIFT) &&
                inputManager.getKeyboardManager().getKeyState(GLFW_KEY_F3) )
            sceneManagerInterface.setNewScene(new Test3DSceneTerrainGeneration(), new String[]{""});

        if (inputManager.getKeyboardManager().getKeyState(GLFW_KEY_LEFT_SHIFT) &&
                inputManager.getKeyboardManager().getKeyState(GLFW_KEY_F4) )
            sceneManagerInterface.setNewScene(new Test2DPlatformerBox2D(), new String[]{""});

    }

    @Override
    public void display() {
        super.setVirtualScene();
        clear();
        super.display();

        title.display();
        guiList.display();
        sceneList.display();

        super.setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();

        if (guiList != null)
            guiList.unload();

        if (sceneList != null)
            sceneList.unload();

        if (title != null)
            title.unload();
    }
}
