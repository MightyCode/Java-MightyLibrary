package MightyLibrary.mightylib.scene;

import MightyLibrary.mightylib.resources.texture.Texture;
import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.main.Context;
import MightyLibrary.mightylib.main.ContextManager;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.sounds.SoundManager;
import MightyLibrary.project.main.Main;
import MightyLibrary.project.main.MainLoop;
import MightyLibrary.mightylib.graphics.shader.ShaderManager;
import MightyLibrary.mightylib.util.commands.Commands;

public class SceneManager {
    private final SceneManagerInterface sceneInterface;
    private Scene currentScene;

    private final MainLoop loop;

    private final Commands commands;
    private final SoundManager soundManager;

    public SceneManager(MainLoop mLoop){
        this.loop = mLoop;

        sceneInterface = new SceneManagerInterface();

        commands  = new Commands();
        soundManager = SoundManager.getInstance();
    }


    public void init(Scene firstScene, String[] firstArguments){
        System.out.println("--Init SceneManager");
        ShaderManager.getInstance().load();
        Resources.getInstance().load();

        sceneInterface.setNewScene(firstScene, firstArguments);

        changeScene();
    }


    public void update(){
        // Command
        if (Main.admin){
            updateMain();

        }

        if(sceneInterface.isWantingChange())
            changeScene();

        if (sceneInterface.WantQuit)
            exit(sceneInterface.ExitStatus);

        currentScene.update();

        soundManager.lateUpdate();
    }


    private void updateMain(){
        Context mainContext = ContextManager.getInstance().getMainContext();
        InputManager mainInputManager = mainContext.getInputManager();

        if (mainInputManager.inputPressed(InputManager.COMMAND) || commands.isWriteCommands)
            commands.writeCommand();

        if (mainInputManager.inputPressed(InputManager.RELOAD_TEXTURE))
            Resources.getInstance().reload(Texture.class);
    }


    public void display(){
        currentScene.display();
    }


    public void dispose(){
        ContextManager.getInstance().dispose();
        currentScene.dispose();
    }



    private void changeScene(){
        if (Main.admin)
            commands.removeSpecificCommand();

        if(currentScene != null){
            System.out.println("--Unload scene" + currentScene.getClass().getName());
            currentScene.unload();
        }

        SoundManager.getInstance().clearAwaitedSong();

        // Assign the new scene
        currentScene = sceneInterface.getNewScene();
        assert currentScene != null;
        currentScene.setSceneManagerInterface(sceneInterface);
        System.out.println("--Init scene" + currentScene.getClass().getName());
        currentScene.init(sceneInterface.getChangeArgs());

        sceneInterface.reset();
    }


    public void exit(int status){
        loop.exit(status);
    }


    public void unload(){
        System.out.println("--Unload SceneManager");

        if (currentScene != null){
            System.out.println("--Unload scene" + currentScene.getClass().getName());
            currentScene.unload();
        }

        ShaderManager.getInstance().unload();
        Resources.getInstance().unload();
    }
}
