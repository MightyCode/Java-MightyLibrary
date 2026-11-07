package io.github.mightycode.mightylib.scenes;

import io.github.mightycode.mightylib.graphics.GLResources;
import io.github.mightycode.mightylib.graphics.utils.GenDeleteResources;
import io.github.mightycode.mightylib.main.MainLoop;
import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.main.Context;
import io.github.mightycode.mightylib.main.ContextManager;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.texture.TextureData;
import io.github.mightycode.mightylib.sounds.SoundManager;
import io.github.mightycode.mightylib.utils.enginecommand.Commands;

import java.util.function.Supplier;

public class SceneManager {
    private final SceneManagerInterface sceneInterface;
    private Scene currentScene;

    private final GLResources glResources;
    private final Resources resources;

    private final Commands commands;
    private final SoundManager soundManager;

    // Package of the first scene, used to find scenes by their simple name
    private String scenesPackage;

    // defaultLoadingScene: displayed while a scene loads, unless the scene overrides createLoadingScene
    public SceneManager(Supplier<TemplateSceneLoading> defaultLoadingScene) {
        glResources = GLResources.getInstance();
        resources = Resources.getInstance();

        sceneInterface = new SceneManagerInterface(defaultLoadingScene);

        commands = new Commands(this);
        soundManager = SoundManager.getInstance();
    }


    public void init(Scene firstScene, String[] firstArguments){
        System.out.println("--Init SceneManager");

        scenesPackage = firstScene.getClass().getPackage().getName();
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

        // MainLoop stops at the end of this tick
        if (sceneInterface.WantQuit)
            return;

        resources.process(15);

        currentScene.updateByManager();
        soundManager.lateUpdate();
    }


    private void updateMain() {
        Context mainContext = ContextManager.getInstance().getMainContext();
        InputManager mainInputManager = mainContext.getInputManager();

        // Commands come from the console thread or the debug server, never block here
        commands.processPendingCommands();

        if (mainInputManager.inputPressed(InputManager.COMMAND))
            System.out.println("Commands are read from the console without pausing the game, write -> help <- to list them");

        if (mainInputManager.inputPressed(InputManager.RELOAD_TEXTURE))
            Resources.getInstance().reloadAllOfType(TextureData.class);
    }

    public void display() {
        currentScene.displayByManager();
    }


    public void postUpdate() {
        ContextManager.getInstance().postUpdate();
        currentScene.postUpdateByManager();
    }

    private void changeScene() {
        if (MainLoop.isAdmin())
            commands.removeSpecificCommand();

        if(currentScene != null) {
            System.out.println("--Unload scene" + currentScene.getClass().getName());
            currentScene.unload();

            for (String batch : currentScene.getInvolvedBatch())
                Resources.getInstance().unloadBatch(batch);

            GenDeleteResources.PrintAll();
        }

        SoundManager.getInstance().clearAwaitedSong();

        // Assign the new scene
        currentScene = sceneInterface.getNewScene();
        assert currentScene != null;
        currentScene.setSceneManagerInterface(sceneInterface);
        System.out.println("--Init scene" + currentScene.getClass().getName());

        for (String batch : currentScene.getInvolvedBatch())
            Resources.getInstance().loadBatch(batch);

        currentScene.initByManager(sceneInterface.getChangeArgs());

        sceneInterface.reset();
    }

    public boolean isExitRequested() {
        return sceneInterface.WantQuit;
    }

    public int getExitStatus() {
        return sceneInterface.ExitStatus;
    }

    public Commands getCommands() {
        return commands;
    }

    public Scene getCurrentScene() {
        return currentScene;
    }

    public boolean isCurrentSceneLoading() {
        return currentScene != null && currentScene.isWaitingForLoading();
    }

    public String getScenesPackage() {
        return scenesPackage;
    }

    // The change is done at the beginning of the next tick
    public void requestSceneChange(Scene scene, String[] args) {
        sceneInterface.setNewScene(scene, args);
    }

    // The program stops at the beginning of the next tick
    public void requestExit(int status) {
        sceneInterface.exit(status);
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
