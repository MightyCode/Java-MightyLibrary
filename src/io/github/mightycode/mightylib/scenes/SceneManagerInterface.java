package io.github.mightycode.mightylib.scenes;

import io.github.mightycode.mightylib.resources.Resources;

import java.util.function.Supplier;

public class SceneManagerInterface {
    private String[] changeArgs;
    private Scene newScene;

    private final Resources resources;
    private final Supplier<TemplateSceneLoading> defaultLoadingScene;

    // Scene whose batches are loaded in advance (preloadScene)
    private Scene preloadedScene;

    boolean WantQuit;
    int ExitStatus;

    SceneManagerInterface(Resources resources, Supplier<TemplateSceneLoading> defaultLoadingScene){
        this.resources = resources;
        this.defaultLoadingScene = defaultLoadingScene;
        reset();
    }

    TemplateSceneLoading createDefaultLoadingScene() {
        return defaultLoadingScene.get();
    }

    /**
     * If scene is null, the current scene will not be changed
     * @param scene The new scene
     * @param args Args
     */
    public void setNewScene(Scene scene, String[] args){
        this.newScene = scene;
        this.changeArgs = args;
    }

    /**
     * Load in advance the batches of a scene that will probably be set next (setNewScene with the same instance):
     * they load after what the current scene wants, and a loading screen does not wait for them. When the scene is
     * set they are already loaded. Any scene change, or a new call, ends the preload of the previous one.
     * Only the resources are loaded (the batches of getInvolvedBatch), init and launch run when the scene is set.
     */
    public void preloadScene(Scene scene) {
        cancelPreload();

        preloadedScene = scene;
        for (String batch : scene.getInvolvedBatch())
            resources.preloadBatch(batch);
    }

    // What the preload loaded stays only if something else wants it (the batches of the scene set since)
    public void cancelPreload() {
        if (preloadedScene == null)
            return;

        for (String batch : preloadedScene.getInvolvedBatch())
            resources.cancelPreloadBatch(batch);

        preloadedScene = null;
    }

    public void reset(){
        WantQuit = false;
        ExitStatus = -1;

        newScene = null;
        changeArgs = new String[]{};
    }

    public void exit(int exitStatus){
        WantQuit = true;
        ExitStatus = exitStatus;
    }

    String[] getChangeArgs(){
        return changeArgs;
    }

    Scene getNewScene(){
        return newScene;
    }

    boolean isWantingChange(){
        return newScene != null;
    }
}
