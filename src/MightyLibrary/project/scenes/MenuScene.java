package MightyLibrary.project.scenes;

import MightyLibrary.mightylib.graphics.GUI.BackgroundlessButton;
import MightyLibrary.mightylib.graphics.GUI.GUIList;
import MightyLibrary.mightylib.graphics.text.ETextAlignment;
import MightyLibrary.mightylib.scene.Scene;
import MightyLibrary.mightylib.util.math.Color4f;
import MightyLibrary.mightylib.util.math.EDirection;
import MightyLibrary.project.lib.ActionId;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;

public class MenuScene extends Scene {
    private GUIList guiList;

    public void init(String[] args) {
        super.init(args);
        /// SCENE INFORMATION ///

        mainCamera.setPos(new Vector3f(0, 0, 0));
        setClearColor(52, 189, 235, 1f);

        /// RENDERERS ///

        Vector2i windowSize = mainContext.getWindow().getInfo().getSizeCopy();

        BackgroundlessButton button = new BackgroundlessButton(mainContext);
        button.Text.setFont("arial")
                .setAlignment(ETextAlignment.Center)
                .setReference(EDirection.None)
                .setPosition(new Vector2f(windowSize.x * 0.5f, windowSize.y * 0.5f))
                .setFontSize(40)
                .setText("->Test2DScene<-");

        button.Text.copyTo(button.OverlapsText);
        button.OverlapsText.setColor(new Color4f(0.3f));

        guiList = new GUIList(mainContext.getInputManager(), mainContext.getMouseManager());
        guiList.GUIs.put(0, button);
    }


    public void update() {
        super.update();

        guiList.update();

        if (guiList.getSelected() == 0 && mainContext.getInputManager().input(ActionId.SHIFT)){
            sceneManagerInterface.setNewScene(new Test2DScene(),  new String[]{""});
        }

        mainCamera.updateView();
    }


    public void display() {
        super.setVirtualScene();
        clear();

        guiList.display();

        super.setAndDisplayRealScene();
    }


    public void unload() {
        super.unload();

        guiList.unload();
    }
}
