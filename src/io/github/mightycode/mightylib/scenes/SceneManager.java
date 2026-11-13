package io.github.mightycode.mightylib.scenes;


import io.github.mightycode.mightylib.graphics.utils.GenDeleteResources;
import io.github.mightycode.mightylib.main.MainLoop;
import io.github.mightycode.mightylib.inputs.EngineActions;
import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.main.Context;
import io.github.mightycode.mightylib.main.WindowContext;
import io.github.mightycode.mightylib.main.Engine;
import io.github.mightycode.mightylib.resources.texture.TextureData;
import io.github.mightycode.mightylib.sounds.SoundManager;

import java.util.function.Supplier;

public class SceneManager {
    private final SceneManagerInterface sceneInterface;
    private Scene currentScene;

    private final Context context;
    // The commands of the engine are run by the scenes of the main context only
    private final boolean runsCommands;
    private final Engine engine;
    private final SoundManager soundManager;

    // Package of the first scene, used to find scenes by their simple name
    private String scenesPackage;

    // defaultLoadingScene: displayed while a scene loads, unless the scene overrides createLoadingScene
    // runsCommands: the main context, which runs the pending commands of the engine at each tick
    public SceneManager(Engine engine, Context context, boolean runsCommands, Supplier<TemplateSceneLoading> defaultLoadingScene) {
        this.context = context;
        this.engine = engine;
        sceneInterface = new SceneManagerInterface(engine.resources(), defaultLoadingScene);

        this.runsCommands = runsCommands;
        soundManager = engine.soundManager();
    }


    public void init(Scene firstScene, String[] firstArguments){
        System.out.println("--Init SceneManager");

        scenesPackage = firstScene.getClass().getPackage().getName();
        sceneInterface.setNewScene(firstScene, firstArguments);

        changeScene();
    }


    public void update() {
        if (runsCommands)
            updateMain();

        if (sceneInterface.isWantingChange())
            changeScene();

        // MainLoop stops at the end of this tick
        if (sceneInterface.WantQuit)
            return;

        currentScene.updateByManager();
    }


    private void updateMain() {
        WindowContext mainWindow = context.getMainWindow();
        InputManager mainInputManager = mainWindow.getInputManager();

        // Commands come from the console, the debug server or the game, never block here
        engine.commands().processPendingCommands();

        if (MainLoop.isDebug() && mainInputManager.inputPressed(EngineActions.RELOAD_TEXTURE))
            engine.resources().reloadAllOfType(TextureData.class);
    }

    public void display() {
        currentScene.displayByManager();
    }


    public void postUpdate() {
        currentScene.postUpdateByManager();
    }

    private void changeScene() {
        Scene previousScene = currentScene;
        if (previousScene != null) {
            System.out.println("--Unload scene" + previousScene.getClass().getName());
            previousScene.unloadByManager();
        }

        soundManager.clearAwaitedSong();

        // Assign the new scene
        currentScene = sceneInterface.getNewScene();
        assert currentScene != null;
        currentScene.setSceneManagerInterface(sceneInterface);
        System.out.println("--Init scene" + currentScene.getClass().getName());

        for (String batch : currentScene.getInvolvedBatch())
            engine.resources().loadBatch(batch);

        // After loadBatch: what the new scene shares with the previous or the preloaded one is never unloaded in
        // between (the GL elements of the batches, released at once, would be sent to the card again)
        if (previousScene != null) {
            for (String batch : previousScene.getInvolvedBatch())
                engine.resources().unloadBatch(batch);

            GenDeleteResources.PrintAll();
        }

        sceneInterface.cancelPreload();

        currentScene.initByManager(sceneInterface.getChangeArgs());

        sceneInterface.reset();
    }

    public boolean isExitRequested() {
        return sceneInterface.WantQuit;
    }

    public int getExitStatus() {
        return sceneInterface.ExitStatus;
    }

    public Scene getCurrentScene() {
        return currentScene;
    }

    // A scene change is asked (by a command or a scene) and will be done at the beginning of the next tick
    public boolean isSceneChangePending() {
        return sceneInterface.isWantingChange();
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
    // The context of this scene manager: its windows and its engine
    public Context getContext() {
        return context;
    }

    // The current scene is unloaded; the resources are freed by the engine at its end
    public void unload(){
        System.out.println("--Unload SceneManager");

        if (currentScene != null){
            System.out.println("--Unload scene" + currentScene.getClass().getName());
            currentScene.unloadByManager();
        }
    }

    // The context is closed while the engine goes on: what its scene loaded must not stay loaded
    public void releaseSceneResources() {
        if (currentScene == null)
            return;

        for (String batch : currentScene.getInvolvedBatch())
            engine.resources().unloadBatch(batch);

        currentScene = null;
    }
}
