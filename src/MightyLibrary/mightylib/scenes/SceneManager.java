package MightyLibrary.mightylib.scenes;

import MightyLibrary.mightylib.graphics.GLResources;
import MightyLibrary.mightylib.main.MainLoop;
import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.main.Context;
import MightyLibrary.mightylib.main.ContextManager;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.resources.texture.TextureData;
import MightyLibrary.mightylib.sounds.SoundManager;
import MightyLibrary.mightylib.utils.enginecommand.Commands;

public class SceneManager {
    private final SceneManagerInterface sceneInterface;
    private Scene currentScene;

    private final GLResources glResources;
    private final Resources resources;

    private final MainLoop.StopLibrary stopLibrary;

    private final Commands commands;
    private final SoundManager soundManager;

    public SceneManager(MainLoop.StopLibrary stopLibrary) {
        glResources = GLResources.getInstance();
        resources = Resources.getInstance();

        this.stopLibrary = stopLibrary;

        sceneInterface = new SceneManagerInterface();

        commands = new Commands();
        soundManager = SoundManager.getInstance();
    }


    public void init(Scene firstScene, String[] firstArguments){
        System.out.println("--Init SceneManager");

        sceneInterface.setNewScene(firstScene, firstArguments);

        changeScene();
    }


    public void update() {
        // Command
        if (MainLoop.isAdmin()) {
            updateMain();
        }

        if (sceneInterface.isWantingChange())
            changeScene();

        if (sceneInterface.WantQuit)
            exit(sceneInterface.ExitStatus);

        glResources.process(); // Create Gl elements, takes a bit of time
        resources.process(15);

        currentScene.updateByManager();
        soundManager.lateUpdate();
    }


    private void updateMain() {
        Context mainContext = ContextManager.getInstance().getMainContext();
        InputManager mainInputManager = mainContext.getInputManager();

        if (mainInputManager.inputPressed(InputManager.COMMAND) || commands.isWriteCommands)
            commands.writeCommand();

        if (mainInputManager.inputPressed(InputManager.RELOAD_TEXTURE))
            Resources.getInstance().reloadAllOfType(TextureData.class);
    }

    public void display() {
        currentScene.displayByManager();
    }


    public void dispose() {
        ContextManager.getInstance().dispose();
        currentScene.disposeByManager();
    }

    private void changeScene() {
        if (MainLoop.isAdmin())
            commands.removeSpecificCommand();

        if(currentScene != null) {
            System.out.println("--Unload scene" + currentScene.getClass().getName());
            currentScene.unload();

            for (String batch : currentScene.getInvolvedBatch())
                Resources.getInstance().unloadBatch(batch);
        }

        SoundManager.getInstance().clearAwaitedSong();

        // Assign the new scene
        currentScene = sceneInterface.getNewScene();
        assert currentScene != null;
        currentScene.setSceneManagerInterface(sceneInterface);
        System.out.println("--Init scene" + currentScene.getClass().getName());

        for (String batch : currentScene.getInvolvedBatch())
            Resources.getInstance().loadBatch(batch);

        currentScene.init(sceneInterface.getChangeArgs());
        currentScene.setWaitForLoading();

        sceneInterface.reset();
    }

    public void exit(int status){
        stopLibrary.exit(status);
    }
    public void unload(){
        System.out.println("--Unload SceneManager");

        if (currentScene != null){
            System.out.println("--Unload scene" + currentScene.getClass().getName());
            currentScene.unload();
        }

        Resources.getInstance().unload();
        GLResources.getInstance().unload();
    }
}
