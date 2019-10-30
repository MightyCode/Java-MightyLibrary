package MightyLibrary.scene;

import MightyLibrary.inputs.InputManager;
import MightyLibrary.inputs.KeyboardManager;
import MightyLibrary.inputs.MouseManager;
import MightyLibrary.main.Main;
import MightyLibrary.main.MainLoop;
import MightyLibrary.render.shader.ShaderManager;
import MightyLibrary.render.texture.TextureManager;
import MightyLibrary.scene.scenes.Scene;
import MightyLibrary.scene.scenes.TestScene;
import MightyLibrary.main.ManagerContainer;
import MightyLibrary.util.commands.Commands;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_F1;

public class SceneManager {
    private ManagerContainer manContainer;

    private Scene currentScene;
    private boolean wantChange;
    private String[] changeArgs;
    private String newSceneName = "";

    private MainLoop loop;

    private Commands commands;

    public SceneManager(MainLoop mLoop){
        this.loop = mLoop;
        manContainer = ManagerContainer.getInstance();

        manContainer.setManager(this);
        manContainer.setManager(new MouseManager()).setManager( new KeyboardManager());
        manContainer.setManager(new InputManager(this.manContainer.keyManager, this. manContainer.mouseManager, new int[][]{{78, 89},{0, 0}}));

        manContainer.setManager(new Camera(120f, new Vector3f(0.0f, 0.0f, 10.0f)));

        manContainer.setManager(new ShaderManager(manContainer.cam));

        manContainer.setManager(new TextureManager());
        // Load every texture

        setNewScene("testScene", new String[]{""});

        commands  = new Commands();
        changeScene();
    }

    public void update(){
        // Command
        if (Main.admin) if(manContainer.keyManager.keyPressed(GLFW_KEY_F1) || commands.isWriteCommands) commands.writeCommand();


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
        if (Main.admin) commands.removeSpecificCommand();

        if(currentScene != null){
            currentScene.unload();
        }

        if(newSceneName.equals("menuscene") || newSceneName.equals("menuScene") || newSceneName.equals("MenuScene")){
            //currentscene = new MenuScreeen(manContainer, changeArgs);
        } else if (newSceneName.equals("testscene") || newSceneName.equals("testScene") || newSceneName.equals("TestScene")){
            currentScene = new TestScene(changeArgs);
        } else {
            System.err.println("Unknown scene : " + newSceneName);
        }

        assert currentScene != null;
        currentScene.init();

        wantChange = false;
        newSceneName = "";
    }

    public void exit(int status){
        loop.exit(status);
    }

    public void unload(){
        currentScene.unload();
        manContainer.shadManager.unload();
        manContainer.texManager.unload();
    }
}
