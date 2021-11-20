package MightyLibrary.mightylib.scene;

import MightyLibrary.mightylib.graphics.texture.Texture;
import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.inputs.KeyboardManager;
import MightyLibrary.mightylib.inputs.MouseManager;
import MightyLibrary.mightylib.graphics.shape.font.TextManager;
import MightyLibrary.mightylib.main.Context;
import MightyLibrary.mightylib.main.ContextManager;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.project.main.Main;
import MightyLibrary.project.main.MainLoop;
import MightyLibrary.mightylib.graphics.shader.ShaderManager;
import MightyLibrary.mightylib.util.commands.Commands;
import org.joml.Vector3f;

public class SceneManager {
    private final SceneManagerInterface sceneInterface;
    private Scene currentScene;

    private final MainLoop loop;

    private final Commands commands;

    public SceneManager(MainLoop mLoop, Scene firstScene, String[] firstArguments){
        this.loop = mLoop;

        sceneInterface = new SceneManagerInterface();
        sceneInterface.setNewScene(firstScene, firstArguments);

        commands  = new Commands();
    }


    public void init(){
        Resources.getInstance().load();
        ShaderManager.getInstance().init();

        changeScene();
    }


    public void update(){
        // Command
        if (Main.admin){
            updateMain();

        }

        if(sceneInterface.isWantingChange())  changeScene();

        currentScene.update();
    }


    private void updateMain(){
        Context mainContext = ContextManager.getInstance().getMainContext();
        InputManager mainInputManager = mainContext.getInputManager();

        if (mainInputManager.inputPressed(InputManager.COMMAND) || commands.isWriteCommands) commands.writeCommand();

        if (mainInputManager.inputPressed(InputManager.RELOAD_TEXTURE)) Resources.getInstance().reload(Texture.class);
    }


    public void display(){
        currentScene.display();
    }


    public void dispose(){
        ContextManager.getInstance().dispose();
        currentScene.dispose();
    }



    private void changeScene(){
        if (Main.admin) commands.removeSpecificCommand();

        if(currentScene != null) currentScene.unload();

        // Assign the new scene
        currentScene = sceneInterface.getNewScene();
        assert currentScene != null;
        currentScene.setSceneManagerInterface(sceneInterface);
        currentScene.init(sceneInterface.getChangeArgs());

        sceneInterface.reset();
    }


    public void exit(int status){
        loop.exit(status);
    }


    public void unload(){
        currentScene.unload();
        ShaderManager.getInstance().unload();
        Resources.getInstance().unload();
    }
}
