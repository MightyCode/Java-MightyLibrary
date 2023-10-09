package MightyLibrary.project.scenes;

import MightyLibrary.mightylib.graphics.renderer.RendererUtils;
import MightyLibrary.mightylib.graphics.renderer._3D.shape.CubeRenderer;
import MightyLibrary.mightylib.graphics.renderer.utils.BasicMaterial;
import MightyLibrary.mightylib.graphics.renderer.utils.LightPoint;
import MightyLibrary.mightylib.graphics.renderer.utils.Material;
import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.main.GameTime;
import MightyLibrary.mightylib.scene.Camera3DCreationInfo;
import MightyLibrary.mightylib.scene.Scene;
import MightyLibrary.mightylib.util.math.Color4f;
import MightyLibrary.mightylib.util.math.ColorList;
import MightyLibrary.mightylib.util.math.MightyMath;
import MightyLibrary.project.main.ActionId;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Test3DScene2 extends Scene {
    private final static Camera3DCreationInfo SCENE_CCI = new Camera3DCreationInfo(120, new Vector3f(0, 4, 0));
    private CubeRenderer[] cubeTexturedMaterialsDirectional;

    private CubeRenderer[] cubeTexturedMaterialsPoint;

    private CubeRenderer lightPoint;

    public Test3DScene2(){
        super(SCENE_CCI);
    }

    public void init(String[] args){
        super.init(args);
        /// SCENE INFORMATION ///

        mainContext.getMouseManager().setCursor(false);
        Color4f clearColor = ColorList.DarkGrey();
        setClearColor(clearColor.getR(), clearColor.getG(), clearColor.getB(), clearColor.getA());

        /// RENDERERS ///

        LightPoint lightMaterial = new LightPoint(Material.Chrome(),
                1.0f, 0.09f, 0.032f);

        Vector3f lightVector = new Vector3f(-0.2f, -1.0f, -0.3f);

        lightPoint = new CubeRenderer("colorShape3D");
        lightPoint.setPosition(new Vector3f(20, 2, 0));
        lightPoint.setScale(new Vector3f(0.3f));
        lightPoint.setColorMode(new Color4f(lightMaterial.Diffuse, 1));
        lightPoint.setNormal();

        Vector3f[] positions = {
            new Vector3f( 0.0f,  0.0f,  0.0f),
            new Vector3f( 2.0f,  5.0f, -15.0f),
            new Vector3f(-1.5f, -2.2f, -2.5f),
            new Vector3f(-3.8f, -2.0f, -12.3f),
            new Vector3f( 2.4f, -0.4f, -3.5f),
            new Vector3f(-1.7f,  3.0f, -7.5f),
            new Vector3f( 1.3f, -2.0f, -2.5f),
            new Vector3f( 1.5f,  2.0f, -2.5f),
            new Vector3f( 1.5f,  0.2f, -1.5f),
            new Vector3f(-1.3f,  1.0f, -1.5f)
        };

        cubeTexturedMaterialsDirectional = new CubeRenderer[positions.length];

        for (int i = 0; i < cubeTexturedMaterialsDirectional.length; ++i){
            CubeRenderer cubeTexturedMaterial = new CubeRenderer("textureMaterial3D");
            cubeTexturedMaterial.setTexturePosition();
            cubeTexturedMaterial.setNormal();
            cubeTexturedMaterial.setMainTextureChannel("container2", "material.diffuse");
            cubeTexturedMaterial.addTextureChannel("container2_specular", "material.specular", 1);

            float angle = MightyMath.toRads(20.0f * i);
            cubeTexturedMaterial.setPosition(positions[i]);
            cubeTexturedMaterial.setRotation(angle, new Vector3f(1.0f, 0.3f, 0.5f).normalize());

            this.cubeTexturedMaterialsDirectional[i] = cubeTexturedMaterial;

            cubeTexturedMaterial
                    .addShaderValue("viewPos", Vector3f.class, main3DCamera.getCamPosRef())
                    .addShaderValue("light.vector", Vector4f.class, RendererUtils.ToLightDirection(lightVector))
                    .addShaderValue("material.shininess", Float.class, 64f);

            ((BasicMaterial)lightMaterial).addToRenderer(cubeTexturedMaterial, "light");
        }

        cubeTexturedMaterialsPoint = new CubeRenderer[positions.length];

        for (int i = 0; i < cubeTexturedMaterialsDirectional.length; ++i){
            CubeRenderer cubeTexturedMaterial = new CubeRenderer("textureMaterial3D");
            cubeTexturedMaterial.setTexturePosition();
            cubeTexturedMaterial.setNormal();
            cubeTexturedMaterial.setMainTextureChannel("container2", "material.diffuse");
            cubeTexturedMaterial.addTextureChannel("container2_specular", "material.specular", 1);

            float angle = MightyMath.toRads(20.0f * i);
            cubeTexturedMaterial.setPosition(positions[i].add(new Vector3f(20, 0, 0), new Vector3f()));
            cubeTexturedMaterial.setRotation(angle, new Vector3f(1.0f, 0.3f, 0.5f).normalize());

            this.cubeTexturedMaterialsPoint[i] = cubeTexturedMaterial;

            cubeTexturedMaterial
                    .addShaderValue("viewPos", Vector3f.class, main3DCamera.getCamPosRef())
                    .addShaderValue("light.vector", Vector4f.class, RendererUtils.ToLightPosition(lightPoint.position()))
                    .addShaderValue("material.shininess", Float.class, 64f);

            lightMaterial.addToRenderer(cubeTexturedMaterial, "light");
        }
    }


    public void update() {
        super.update();

        if (mainContext.getInputManager().inputPressed(ActionId.ESCAPE))
            sceneManagerInterface.setNewScene(new MenuScene(), new String[]{});

        InputManager inputManager = mainContext.getInputManager();

        int speed = 8;
        if (inputManager.getState(ActionId.SHIFT))
            speed = 16;

        if (inputManager.getState(ActionId.MOVE_LEFT))
            main3DCamera.speedAngX(main3DCamera.getSpeed().x * speed * GameTime.DeltaTime());

        if (inputManager.getState(ActionId.MOVE_RIGHT))
            main3DCamera.speedAngX(-main3DCamera.getSpeed().x * speed * GameTime.DeltaTime());

        if (inputManager.getState(ActionId.MOVE_FORWARD))
            main3DCamera.speedAngZ(-main3DCamera.getSpeed().z * speed * GameTime.DeltaTime());

        if (inputManager.getState(ActionId.MOVE_BACKWARD))
            main3DCamera.speedAngZ(main3DCamera.getSpeed().z * speed * GameTime.DeltaTime());

        if (inputManager.getState(ActionId.MOVE_UP))
            main3DCamera.setY(main3DCamera.getCamPosRef().y + main3DCamera.getSpeed().y * speed * GameTime.DeltaTime());

        if (inputManager.getState(ActionId.MOVE_DOWN))
            main3DCamera.setY(main3DCamera.getCamPosRef().y - main3DCamera.getSpeed().y * speed * GameTime.DeltaTime());


        if (inputManager.inputPressed(ActionId.TAB)) {
            main3DCamera.invertLockViewCursor();
            mainContext.getMouseManager().invertCursorState();
        }

        for (int i = 0; i < cubeTexturedMaterialsDirectional.length; ++i) {
            cubeTexturedMaterialsDirectional[i].updateShaderValue("viewPos", main3DCamera.getCamPosRef());
            //cubeTexturedMaterial.updateShaderValue("light.position", light.position());

            cubeTexturedMaterialsPoint[i].updateShaderValue("viewPos", main3DCamera.getCamPosRef());
        }

        main3DCamera.updateView();
    }


    public void display() {
        super.setVirtualScene();
        clear();

        // Better to draw the world here
        for (int i = 0; i < cubeTexturedMaterialsDirectional.length; ++i) {
            cubeTexturedMaterialsDirectional[i].display();
            cubeTexturedMaterialsPoint[i].display();
        }

        lightPoint.display();

        super.setAndDisplayRealScene();
    }


    public void unload(){
        super.unload();

        for (int i = 0; i < cubeTexturedMaterialsDirectional.length; ++i) {
            cubeTexturedMaterialsDirectional[i].unload();
            cubeTexturedMaterialsPoint[i].unload();
        }

        lightPoint.unload();
    }
}