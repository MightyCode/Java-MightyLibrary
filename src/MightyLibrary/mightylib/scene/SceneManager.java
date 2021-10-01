package MightyLibrary.mightylib.scene;

import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.inputs.KeyboardManager;
import MightyLibrary.mightylib.inputs.MouseManager;
import MightyLibrary.mightylib.render.shape.font.TextManager;
import MightyLibrary.project.main.Main;
import MightyLibrary.project.main.MainLoop;
import MightyLibrary.mightylib.render.shader.ShaderManager;
import MightyLibrary.mightylib.render.texture.TextureManager;
import MightyLibrary.mightylib.main.ManagerContainer;
import MightyLibrary.mightylib.util.commands.Commands;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_F1;

public class SceneManager {
    private ManagerContainer manContainer;

    private Scene currentScene;
    private boolean wantChange;
    private String[] changeArgs;
    private Scene newScene;

    private MainLoop loop;

    private Commands commands;

    public SceneManager(MainLoop mLoop){
        this.loop = mLoop;
        manContainer = ManagerContainer.getInstance();

        manContainer.setManager(this);
        manContainer.setManager(new MouseManager()).setManager( new KeyboardManager());
        manContainer.setManager(new InputManager(this.manContainer.keyManager, this.manContainer.mouseManager));
        manContainer.inpManager.init(new int[0][]);

        manContainer.setManager(new Camera(120f, new Vector3f(0.0f, 0.0f, 10.0f)));

        manContainer.setManager(new ShaderManager(manContainer.cam));

        // Load every texture
        manContainer.setManager(new TextureManager());

        manContainer.setManager(new TextManager());


        setNewScene(new Scene(), new String[]{""});

        commands  = new Commands();
    }


    public void init(){
        changeScene();
    }


    public void update(){
        // Command
        if (Main.admin){
            updateMain();

        }

        if(wantChange)  changeScene();

        currentScene.update();
    }


    private void updateMain(){
        if (manContainer.inpManager.inputPressed(InputManager.COMMAND) || commands.isWriteCommands) commands.writeCommand();

        if (manContainer.inpManager.inputPressed(InputManager.RELOAD_TEXTURE)) manContainer.textureManager.reload();
    }


    public void display(){
        currentScene.display();
    }


    public void dispose(){
        manContainer.shadManager.dispose();
        manContainer.inpManager.dispose();
    }


    public void setNewScene(Scene scene, String[] args){
        this.newScene = scene;
        this.changeArgs = args;
        this.wantChange = true;
    }


    private void changeScene(){
        if (Main.admin) commands.removeSpecificCommand();

        if(currentScene != null) currentScene.unload();

        // Assign the new scene
        currentScene = newScene;
        assert currentScene != null;
        currentScene.init(changeArgs);

        wantChange = false;
        newScene = null;
    }


    public void exit(int status){
        loop.exit(status);
    }


    public void unload(){
        currentScene.unload();
        manContainer.shadManager.unload();
        manContainer.textureManager.unload();
    }
}
