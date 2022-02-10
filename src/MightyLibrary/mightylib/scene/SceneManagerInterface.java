package MightyLibrary.mightylib.scene;

public class SceneManagerInterface {
    private String[] changeArgs;
    private Scene newScene;

    boolean WantQuit;
    int ExitStatus;

    public SceneManagerInterface(){
        reset();
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
        changeArgs = new String[]{""};
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
