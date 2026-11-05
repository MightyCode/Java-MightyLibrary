package MightyLibrary.project.scenes;

import MightyLibrary.mightylib.graphics.lightning.MultiLightningManager;
import MightyLibrary.mightylib.graphics.lightning.lights.DirectionalLight;
import MightyLibrary.mightylib.graphics.lightning.lights.LightDecrease;
import MightyLibrary.mightylib.graphics.lightning.lights.PointLight;
import MightyLibrary.mightylib.graphics.lightning.lights.SpotLight;
import MightyLibrary.mightylib.graphics.lightning.materials.BasicMaterial;
import MightyLibrary.mightylib.graphics.renderer._3D.ModelRenderer;
import MightyLibrary.mightylib.graphics.renderer._3D.shape.CubeRenderer;
import MightyLibrary.mightylib.graphics.text.ETextAlignment;
import MightyLibrary.mightylib.graphics.text.Text;
import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.main.utils.GameTime;
import MightyLibrary.mightylib.resources.models.Model;
import MightyLibrary.mightylib.scenes.Scene;
import MightyLibrary.mightylib.scenes.camera.Camera2D;
import MightyLibrary.mightylib.scenes.camera.Camera3DCreationInfo;
import MightyLibrary.mightylib.utils.math.color.Color4f;
import MightyLibrary.mightylib.utils.math.color.ColorList;
import MightyLibrary.mightylib.utils.math.geometry.EDirection;
import MightyLibrary.project.main.ActionId;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;

/**
 * Model loaded by Assimp (backpack of the demonstration package, with its .mtl), lit by several lights:
 * a directional light, coloured point lights around it and a spot light following the camera.
 */
public class Test3DModelLoading extends Scene {
    private static final String MODEL_NAME = "backpack";
    private static final Camera3DCreationInfo SCENE_CCI = new Camera3DCreationInfo(90, new Vector3f(0, 0, 6));

    private ModelRenderer model;
    private MultiLightningManager lightManager;
    private CubeRenderer[] lightCubes;

    private Camera2D hudCamera;
    private Text info;

    public Test3DModelLoading() {
        super(SCENE_CCI);
    }

    @Override
    protected String[] getInvolvedBatch() {
        return new String[]{
                "elements3d"
        };
    }

    @Override
    public void launch(String[] args) {
        Color4f clearColor = ColorList.DarkGrey();
        setClearColor(clearColor.getR(), clearColor.getG(), clearColor.getB(), clearColor.getA());

        hudCamera = main2DCamera.copy();
        info = new Text();
        info.setReferenceCamera(hudCamera);
        Vector2i size = mainContext.getWindow().getInfo().getSizeRef();
        info.setFont("bahnschrift")
                .setFontSize(20)
                .setReference(EDirection.LeftUp)
                .setAlignment(ETextAlignment.Left)
                .setPosition(new Vector2f(5, 5));

        Model data = resources.getResource(Model.class, MODEL_NAME);
        if (data == null || !data.isLoaded()) {
            String message = "Model '" + MODEL_NAME + "' missing: copy the demonstration package into resources/models/backpack/";
            System.err.println("--Test3DModelLoading : " + message);
            info.setText(message);
            return;
        }

        info.setText(MODEL_NAME + " : " + data.getNumberOfMeshes() + " mesh(es), " + data.getNumberOfVertices()
                + " vertices - TAB : free the cursor");

        mainContext.getMouseManager().setCursor(false);

        model = new ModelRenderer(MODEL_NAME);
        model.addShaderValue("viewPos", Vector3f.class, main3DCamera.getCamPosRef());

        LightDecrease lightDecrease = new LightDecrease(1.0f, 0.09f, 0.032f);

        lightManager = new MultiLightningManager("dirLight", "pointLights", "spotLight");
        lightManager.setDirectionalLight(new DirectionalLight(new BasicMaterial(
                new Vector3f(0.05f), new Vector3f(0.3f), new Vector3f(0.3f)))
                .setDirection(new Vector3f(-0.2f, -1.0f, -0.3f)));

        lightManager.setSpotLight(new SpotLight(new BasicMaterial(
                new Vector3f(0f), new Vector3f(0.6f), new Vector3f(0.8f)), lightDecrease)
                .setPosition(main3DCamera.getCamPosRef()).setDirection(main3DCamera.getLookAtVector())
                .enable());

        Vector3f[] positions = {
                new Vector3f(3f, 1f, 2f),
                new Vector3f(-3f, 1f, 2f),
                new Vector3f(0f, 3f, -3f)
        };
        Vector3f[] colors = {
                new Vector3f(1f, 0.3f, 0.3f),
                new Vector3f(0.3f, 1f, 0.3f),
                new Vector3f(0.3f, 0.3f, 1f)
        };

        lightCubes = new CubeRenderer[positions.length];
        for (int i = 0; i < positions.length; ++i) {
            lightManager.addPointLight(new PointLight(new BasicMaterial(
                    new Vector3f(colors[i]).mul(0.05f), colors[i], colors[i]), lightDecrease)
                    .setPosition(positions[i]));

            lightCubes[i] = new CubeRenderer("colorShape3D");
            lightCubes[i].setPosition(positions[i]);
            lightCubes[i].setScale(new Vector3f(0.2f));
            lightCubes[i].setColorMode(new Color4f(colors[i], 1));
            lightCubes[i].setNormal();
        }

        lightManager.addRenderer(model);
    }

    @Override
    public void update() {
        super.update();

        InputManager inputManager = mainContext.getInputManager();
        if (inputManager.inputPressed(ActionId.ESCAPE))
            sceneManagerInterface.setNewScene(new MenuScene(), new String[]{});

        if (model == null)
            return;

        int speed = inputManager.getState(ActionId.SHIFT) ? 8 : 4;

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

        model.updateShaderValue("viewPos", main3DCamera.getCamPosRef());
        lightManager.getSpotLight().setPosition(main3DCamera.getCamPosRef()).setDirection(main3DCamera.getLookAtVector());
        lightManager.update();

        main3DCamera.updateView();
    }

    @Override
    public void display() {
        super.setVirtualScene();
        clear();

        if (model != null) {
            model.display();
            for (CubeRenderer cube : lightCubes)
                cube.display();
        }

        super.display();

        info.display();

        super.setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();

        if (model != null) {
            model.unload();
            for (CubeRenderer cube : lightCubes)
                cube.unload();
        }

        info.unload();
    }
}
