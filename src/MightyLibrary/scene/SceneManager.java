package MightyLibrary.scene;

import MightyLibrary.inputs.InputManager;
import MightyLibrary.inputs.KeyboardManager;
import MightyLibrary.inputs.MouseManager;
import MightyLibrary.main.Window;
import MightyLibrary.render.shader.ShaderManager;
import MightyLibrary.render.texture.TextureManager;
import MightyLibrary.scene.scenes.Scene;
import MightyLibrary.scene.scenes.TestScene;
import MightyLibrary.main.ManagerContainer;
import org.joml.Vector3f;

public class SceneManager {
    private ManagerContainer manContainer;

    private Scene currentScene;
    private boolean wantChange;
    private String[] changeArgs;
    private String newSceneName = "";

    private Window window;

    public SceneManager(Window window){
        this.window = window;
        manContainer = ManagerContainer.getInstance();

        manContainer.setManager(this);
        manContainer.setManager(new MouseManager(this.manContainer.wParams)).setManager( new KeyboardManager(this.manContainer.wParams));
        manContainer.setManager(new InputManager(this.manContainer.keyManager,this. manContainer.mouseManager, new int[][]{{78, 89},{0, 0}}));

        manContainer.setManager(new Camera(70f, new Vector3f(0.0f, 0.0f, 10.0f)));

        manContainer.setManager(new ShaderManager(manContainer.cam));

        manContainer.setManager(new TextureManager());
        // Load every texture
        manContainer.texManager.init();

        setNewScene("testScene", new String[]{""});
        changeScene();
    }

    public void update(){
        if(wantChange){
            changeScene();
        }

        currentScene.update();
    }

    public void display(){
        currentScene.display();
    }

    public void dispose(){
        manContainer.shadManager.dispose();
        manContainer.inpManager.dispose();
    }

    public void setNewScene(String sceneName, String[] args){
        this.newSceneName = sceneName;
        this.changeArgs = args;
        this.wantChange = true;
    }

    private void changeScene(){
        if(currentScene != null){
            currentScene.unload();
        }

        if(newSceneName.equals("menuscene") || newSceneName.equals("menuScene") || newSceneName.equals("MenuScene")){
            //currentscene = new MenuScreeen(manContainer, changeArgs);
        }

        if(newSceneName.equals("testscene") || newSceneName.equals("testScene") || newSceneName.equals("TestScene")){
            currentScene = new TestScene(changeArgs);
        }

        assert currentScene != null;
        currentScene.init();

        wantChange = false;
        newSceneName = "";
    }

    public void exit(){
        window.exit();
    }

    public void unload(){
        currentScene.unload();
        manContainer.shadManager.unload();
        manContainer.texManager.unload();
    }
}
