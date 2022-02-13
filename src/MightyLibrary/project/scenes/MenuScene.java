package MightyLibrary.project.scenes;

import MightyLibrary.mightylib.graphics.GUI.BackgroundlessButton;
import MightyLibrary.mightylib.graphics.GUI.GUIList;
import MightyLibrary.mightylib.graphics.text.ETextAlignment;
import MightyLibrary.mightylib.scene.Scene;
import MightyLibrary.mightylib.sounds.SoundData;
import MightyLibrary.mightylib.sounds.SoundManager;
import MightyLibrary.mightylib.sounds.SoundSource;
import MightyLibrary.mightylib.sounds.SoundSourceCreationInfo;
import MightyLibrary.mightylib.util.math.Color4f;
import MightyLibrary.mightylib.util.math.EDirection;
import MightyLibrary.mightylib.util.math.MightyMath;
import MightyLibrary.mightylib.physics.tweenings.ETweeningBehaviour;
import MightyLibrary.mightylib.physics.tweenings.ETweeningOption;
import MightyLibrary.mightylib.physics.tweenings.ETweeningType;
import MightyLibrary.mightylib.physics.tweenings.type.FloatTweening;
import MightyLibrary.project.lib.ActionId;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.lwjgl.openal.AL10;

public class MenuScene extends Scene {
    private GUIList guiList;

    BackgroundlessButton buttonCollisionTest;
    BackgroundlessButton buttonQuit;

    FloatTweening rotation;

    SoundSource sound;

    public void init(String[] args) {
        super.init(args);
        /// SCENE INFORMATION ///

        main3DCamera.setPos(new Vector3f(0, 0, 0));
        setClearColor(52, 189, 235, 1f);

        /// RENDERERS ///

        Vector2i windowSize = mainContext.getWindow().getInfo().getSizeCopy();

        BackgroundlessButton button2DScene = new BackgroundlessButton(mainContext);
        button2DScene.Text.setFont("arial")
                .setAlignment(ETextAlignment.Center)
                .setReference(EDirection.None)
                .setPosition(new Vector2f(windowSize.x * 0.5f, windowSize.y * 0.5f))
                .setFontSize(40)
                .setText("Test2DScene");


        button2DScene.Text.copyTo(button2DScene.OverlapsText);
        button2DScene.OverlapsText.setColor(new Color4f(0.3f))
                .setText("->Test2DScene<-");

        BackgroundlessButton button3DScene = button2DScene.copy();
        button3DScene.Text.setPosition(new Vector2f(windowSize.x * 0.5f, windowSize.y * 0.6f))
                        .setText("Test3DScene");

        button3DScene.Text.copyTo(button3DScene.OverlapsText);
        button3DScene.OverlapsText.setColor(new Color4f(0.3f))
                .setText("->Test3DScene<-");

        buttonCollisionTest = button2DScene.copy();
        buttonCollisionTest.Text.setPosition(new Vector2f(windowSize.x * 0.5f, windowSize.y * 0.7f))
                .setText("TestCollisionScene");

        buttonCollisionTest.Text.copyTo(buttonCollisionTest.OverlapsText);
        buttonCollisionTest.OverlapsText.setColor(new Color4f(0.3f))
                .setText("->TestCollisionScene<-");

        buttonQuit = button2DScene.copy();
        buttonQuit.Text.setPosition(new Vector2f(windowSize.x * 0.5f, windowSize.y * 0.9f))
                .setText("Quit");

        buttonQuit.Text.copyTo(buttonQuit.OverlapsText);
        buttonQuit.OverlapsText.setColor(new Color4f(0.3f))
                .setText("->Quit<-");

        guiList = new GUIList(mainContext.getInputManager(), mainContext.getMouseManager());
        guiList.setupActionInputValues(ActionId.SELECT_UP, ActionId.SELECT_DOWN);
        guiList.GUIs.put(0, button2DScene);
        guiList.GUIs.put(1, button3DScene);
        guiList.GUIs.put(2, buttonCollisionTest);
        guiList.GUIs.put(3, buttonQuit);
        guiList.ShouldLoop = false;

        rotation = new FloatTweening();
        rotation.setTweeningOption(ETweeningOption.LoopReversed)
                .setTweeningValues(ETweeningType.Sinusoidal, ETweeningBehaviour.InOut)
                .initTwoValue(2, 0f, MightyMath.PI_FLOAT * 2f);

        SoundSourceCreationInfo creationInfo = new SoundSourceCreationInfo();
        creationInfo.name = "music";
        creationInfo.loop = true;
        creationInfo.gain = 1f;
        creationInfo.position = new Vector3f(100, 100, 100);
        creationInfo.relative = false;

        sound = SoundManager.getInstance().createSoundSource(creationInfo);
        SoundManager.getInstance().getListener().move2D(new Vector2f(100, 1));
    }


    public void update() {
        super.update();

        guiList.update();

        if (mainContext.getInputManager().input(ActionId.ENTER) || mainContext.getInputManager().inputPressed(ActionId.LEFT_CLICK)){
            Integer id = guiList.getSelected();
            if (id != null) {
                switch (id) {
                    case 0:
                        sceneManagerInterface.setNewScene(new Test2DScene(), new String[]{""});
                        break;
                    case 1:
                        sceneManagerInterface.setNewScene(new Test3DScene(), new String[]{""});
                        break;
                    case 2:
                        sceneManagerInterface.setNewScene(new TestCollisionSystem(), new String[]{""});
                        break;
                    case 3:
                        sceneManagerInterface.exit(0);
                        break;
                }
            }
        }

        rotation.update();
        buttonQuit.Text.setRotation(rotation.value(), new Vector3f(0, 0, 1));

        buttonCollisionTest.OverlapsText.setX(rotation.value() * 50);
    }


    public void display() {
        super.setVirtualScene();
        clear();

        guiList.display();

        super.setAndDisplayRealScene();
    }


    public void unload() {
        super.unload();
        SoundManager.getInstance().remove(sound);

        guiList.unload();
    }
}
