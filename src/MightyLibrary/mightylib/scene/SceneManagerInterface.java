package MightyLibrary.mightylib.scene;

class SceneManagerInterface {
    private String[] changeArgs;
    private Scene newScene;

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
        newScene = null;
        changeArgs = new String[]{""};
    }

    public String[] getChangeArgs(){
        return changeArgs;
    }

    public Scene getNewScene(){
        return newScene;
    }

    public boolean isWantingChange(){
        return newScene != null;
    }
}
