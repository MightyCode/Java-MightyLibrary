package MightyLibrary.mightylib.scenes;

import java.util.function.Supplier;

public class SceneManagerInterface {
    private String[] changeArgs;
    private Scene newScene;

    private final Supplier<TemplateSceneLoading> defaultLoadingScene;

    boolean WantQuit;
    int ExitStatus;

    SceneManagerInterface(Supplier<TemplateSceneLoading> defaultLoadingScene){
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
